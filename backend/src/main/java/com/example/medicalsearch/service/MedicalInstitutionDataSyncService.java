package com.example.medicalsearch.service;

import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.DepartmentPage;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.FullDataPage;
import com.example.medicalsearch.client.PublicDataRateLimitException;
import com.example.medicalsearch.config.AppProperties;
import com.example.medicalsearch.entity.DataSyncStatus;
import com.example.medicalsearch.entity.HospitalDepartment;
import com.example.medicalsearch.repository.MedicalInstitutionSyncWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MedicalInstitutionDataSyncService {

    private static final Logger log = LogManager.getLogger(MedicalInstitutionDataSyncService.class);
    private static final int MAX_SNAPSHOT_ATTEMPTS = 3;

    private final NationalMedicalCenterFullDataClient fullDataClient;
    private final MedicalInstitutionSyncWriter syncWriter;
    private final AppProperties appProperties;
    private final AtomicBoolean synchronizationRunning = new AtomicBoolean(false);
    private final AtomicBoolean synchronizationRequested = new AtomicBoolean(false);

    public MedicalInstitutionDataSyncService(
            NationalMedicalCenterFullDataClient fullDataClient,
            MedicalInstitutionSyncWriter syncWriter,
            AppProperties appProperties
    ) {
        this.fullDataClient = fullDataClient;
        this.syncWriter = syncWriter;
        this.appProperties = appProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeOnStartup() {
        if (!fullDataClient.isEnabled()) {
            log.info("PUBLIC_DATA_ENABLED=false여서 병·의원 FullData 동기화를 건너뜁니다.");
            return;
        }
        boolean databaseEmpty = syncWriter.isMedicalInstitutionTableEmpty();
        boolean initialSyncIncomplete = databaseEmpty || !syncWriter.hasCompletedFullDataSync();
        if (!initialSyncIncomplete && !appProperties.publicData().syncOnStartup()) {
            return;
        }
        LocalDate today = LocalDate.now(appProperties.timeZone());
        if (!initialSyncIncomplete
                && syncWriter.hasSuccessfulHospitalBaseSyncSince(today.atStartOfDay())) {
            return;
        }
        if (databaseEmpty) {
            log.info("의료기관 DB가 비어 있어 병·의원 FullData 초기 동기화를 시작합니다.");
        } else if (initialSyncIncomplete) {
            log.info("완료된 FullData 동기화 이력이 없어 부분 적재 데이터를 이어서 동기화합니다.");
        }
        CompletableFuture.runAsync(this::synchronize);
    }

    @Scheduled(
            cron = "${app.public-data.sync-cron:0 0 3 * * *}",
            zone = "${app.time-zone:Asia/Seoul}"
    )
    public void synchronize() {
        if (!fullDataClient.isEnabled()) {
            log.info("PUBLIC_DATA_ENABLED=false여서 병·의원 FullData 동기화를 건너뜁니다.");
            return;
        }
        if (!synchronizationRunning.compareAndSet(false, true)) {
            log.info("병·의원 FullData 동기화가 이미 실행 중입니다.");
            return;
        }

        String syncRunId = UUID.randomUUID().toString();
        LocalDateTime startedAt = LocalDateTime.now(appProperties.timeZone());
        try {
            int institutionCount = synchronizeFullData(syncRunId, startedAt);
            int inactiveCount = syncWriter.deactivateMissingHospitals(syncRunId, startedAt);
            String baseMessage = "기관 upsert=" + institutionCount
                    + ", 미수신 기관 비활성화=" + inactiveCount;
            syncWriter.recordHospitalBaseHistory(startedAt, baseMessage);

            int departmentRelationCount;
            try {
                departmentRelationCount = synchronizeDepartments(syncRunId);
            } catch (PublicDataRateLimitException exception) {
                String message = baseMessage
                        + ", 진료과목 동기화 보류=" + exception.getMessage();
                recordFailure(startedAt, message);
                log.warn(
                        "병·의원 기본 데이터 동기화는 완료했습니다. "
                                + "진료과목 API 요청 한도가 복구되면 다음 일일 동기화에서 재시도합니다: {}",
                        exception.getMessage()
                );
                return;
            }

            int staleDepartmentCount = syncWriter.deleteStaleDepartments(syncRunId);
            String message = "기관 upsert=" + institutionCount
                    + ", 진료과목 관계 upsert=" + departmentRelationCount
                    + ", 미수신 기관 비활성화=" + inactiveCount
                    + ", 만료 진료과목 관계 삭제=" + staleDepartmentCount;
            syncWriter.recordHistory(DataSyncStatus.SUCCESS, startedAt, message);
            log.info("병·의원 FullData 동기화 완료: {}", message);
        } catch (PublicDataRateLimitException exception) {
            String message = "공공데이터 요청 한도 초과: " + exception.getMessage();
            recordFailure(startedAt, message);
            log.warn(
                    "병·의원 FullData API 요청 한도가 복구되면 다음 일일 동기화에서 재시도합니다: {}",
                    exception.getMessage()
            );
        } catch (RuntimeException exception) {
            String message = "동기화 실패: " + exception.getMessage();
            recordFailure(startedAt, message);
            log.error("병·의원 FullData 동기화 실패. 기존 활성 상태는 유지합니다.", exception);
        } finally {
            synchronizationRunning.set(false);
        }
    }

    public boolean requestSynchronization() {
        if (!fullDataClient.isEnabled()
                || synchronizationRunning.get()
                || !synchronizationRequested.compareAndSet(false, true)) {
            return false;
        }
        CompletableFuture.runAsync(() -> {
            try {
                synchronize();
            } finally {
                synchronizationRequested.set(false);
            }
        });
        return true;
    }

    public boolean isSynchronizationRunning() {
        return synchronizationRunning.get() || synchronizationRequested.get();
    }

    private int synchronizeFullData(String syncRunId, LocalDateTime syncedAt) {
        return synchronizeStableSnapshot(
                "병·의원 FullData",
                () -> synchronizeFullDataAttempt(syncRunId, syncedAt)
        );
    }

    private int synchronizeFullDataAttempt(String syncRunId, LocalDateTime syncedAt) {
        int pageNumber = 1;
        int expectedTotalCount = -1;
        int maxPageCount = Integer.MAX_VALUE;
        int receivedItemCount = 0;
        Set<String> receivedHpids = new HashSet<>();

        do {
            FullDataPage page = fullDataClient.fetchFullDataPage(pageNumber);
            if (pageNumber == 1 && (page.totalCount() <= 0 || page.items().isEmpty())) {
                throw new IllegalStateException(
                        "병·의원 FullData 전체 건수가 0이어서 기존 기관을 비활성화하지 않습니다."
                );
            }
            if (pageNumber == 1) {
                expectedTotalCount = page.totalCount();
                maxPageCount = expectedPageCount(expectedTotalCount) + 1;
            } else if (page.totalCount() != expectedTotalCount) {
                throw new SnapshotChangedException(
                        "page=" + pageNumber
                                + ", 시작 전체 건수=" + expectedTotalCount
                                + ", 현재 전체 건수=" + page.totalCount()
                );
            }
            if (page.items().isEmpty() && receivedItemCount < expectedTotalCount) {
                throw new IllegalStateException("병·의원 FullData 페이지가 전체 건수 전에 비었습니다.");
            }
            int uniqueCountBeforePage = receivedHpids.size();
            syncWriter.upsertInstitutions(page.items(), syncRunId, syncedAt);
            receivedItemCount += page.items().size();
            page.items().forEach(institution -> receivedHpids.add(institution.hpid()));
            log.info(
                    "병·의원 FullData 페이지 저장: page={}, 페이지 건수={}, "
                            + "누적 수신={}/{}, 고유 HPID={}",
                    pageNumber,
                    page.items().size(),
                    receivedItemCount,
                    expectedTotalCount,
                    receivedHpids.size()
            );
            if (!page.items().isEmpty() && receivedHpids.size() == uniqueCountBeforePage) {
                throw new IllegalStateException("병·의원 FullData에 동일한 페이지가 반복되었습니다.");
            }
            if (receivedItemCount > expectedTotalCount) {
                throw new IllegalStateException(
                        "병·의원 FullData 수신 행 수가 전체 건수를 초과했습니다: "
                                + receivedItemCount + "/" + expectedTotalCount
                );
            }
            pageNumber++;
            if (pageNumber > maxPageCount && receivedItemCount < expectedTotalCount) {
                throw new IllegalStateException("병·의원 FullData에 중복 페이지가 반복되었습니다.");
            }
        } while (receivedItemCount < expectedTotalCount);

        int duplicateHpidCount = receivedItemCount - receivedHpids.size();
        if (duplicateHpidCount > 0) {
            log.warn(
                    "병·의원 FullData에 중복 HPID가 있어 고유 기관 기준으로 저장했습니다: "
                            + "수신 행={}, 고유 HPID={}, 중복 행={}",
                    receivedItemCount,
                    receivedHpids.size(),
                    duplicateHpidCount
            );
        }
        return receivedHpids.size();
    }

    private int synchronizeDepartments(String syncRunId) {
        int relationCount = 0;
        for (HospitalDepartment department : HospitalDepartment.values()) {
            if (department.getPublicDataCode() == null) {
                continue;
            }
            int departmentRelationCount = synchronizeDepartment(department, syncRunId);
            relationCount += departmentRelationCount;
            log.info(
                    "진료과목 동기화 완료: code={}, name={}, 관계={}",
                    department.getPublicDataCode(),
                    department.getOfficialName(),
                    departmentRelationCount
            );
        }
        return relationCount;
    }

    private int synchronizeDepartment(
            HospitalDepartment department,
            String syncRunId
    ) {
        return synchronizeStableSnapshot(
                "진료과목 " + department.getPublicDataCode(),
                () -> synchronizeDepartmentAttempt(department, syncRunId)
        );
    }

    private int synchronizeDepartmentAttempt(
            HospitalDepartment department,
            String syncRunId
    ) {
        int pageNumber = 1;
        int expectedTotalCount = -1;
        int maxPageCount = Integer.MAX_VALUE;
        int receivedItemCount = 0;
        Set<String> receivedHpids = new HashSet<>();
        do {
            DepartmentPage page = fullDataClient.fetchDepartmentPage(
                    department.getPublicDataCode(),
                    pageNumber
            );
            if (pageNumber == 1) {
                expectedTotalCount = page.totalCount();
                maxPageCount = expectedPageCount(expectedTotalCount) + 1;
            } else if (page.totalCount() != expectedTotalCount) {
                throw new SnapshotChangedException(
                        "page=" + pageNumber
                                + ", 시작 전체 건수=" + expectedTotalCount
                                + ", 현재 전체 건수=" + page.totalCount()
                );
            }
            if (page.itemCount() == 0 && receivedItemCount < expectedTotalCount) {
                throw new IllegalStateException(
                        department.getPublicDataCode() + " 진료과목 페이지가 전체 건수 전에 비었습니다."
                );
            }
            int uniqueCountBeforePage = receivedHpids.size();
            syncWriter.upsertDepartments(
                    department.getPublicDataCode(),
                    page.hpids(),
                    syncRunId
            );
            receivedItemCount += page.itemCount();
            receivedHpids.addAll(page.hpids());
            if (page.itemCount() > 0 && receivedHpids.size() == uniqueCountBeforePage) {
                throw new IllegalStateException(
                        department.getPublicDataCode() + " 진료과목에 동일한 페이지가 반복되었습니다."
                );
            }
            if (receivedItemCount > expectedTotalCount) {
                throw new IllegalStateException(
                        department.getPublicDataCode() + " 진료과목 수신 행 수가 전체 건수를 초과했습니다."
                );
            }
            pageNumber++;
            if (pageNumber > maxPageCount && receivedItemCount < expectedTotalCount) {
                throw new IllegalStateException(
                        department.getPublicDataCode() + " 진료과목에 중복 페이지가 반복되었습니다."
                );
            }
        } while (receivedItemCount < expectedTotalCount);
        int duplicateHpidCount = receivedItemCount - receivedHpids.size();
        if (duplicateHpidCount > 0) {
            log.warn(
                    "진료과목 데이터에 중복 HPID가 있어 고유 관계 기준으로 저장했습니다: "
                            + "code={}, 수신 행={}, 고유 HPID={}, 중복 행={}",
                    department.getPublicDataCode(),
                    receivedItemCount,
                    receivedHpids.size(),
                    duplicateHpidCount
            );
        }
        return receivedHpids.size();
    }

    private int synchronizeStableSnapshot(String label, IntSupplier synchronization) {
        for (int attempt = 1; attempt <= MAX_SNAPSHOT_ATTEMPTS; attempt++) {
            try {
                return synchronization.getAsInt();
            } catch (SnapshotChangedException exception) {
                if (attempt == MAX_SNAPSHOT_ATTEMPTS) {
                    throw new IllegalStateException(
                            label + " 전체 건수가 계속 변경되어 "
                                    + MAX_SNAPSHOT_ATTEMPTS
                                    + "회 수집 시도 후 중단했습니다. 기존 활성 데이터는 유지합니다.",
                            exception
                    );
                }
                log.warn(
                        "{} 전체 건수가 수집 도중 변경되어 첫 페이지부터 다시 시도합니다: "
                                + "시도={}/{}, {}",
                        label,
                        attempt + 1,
                        MAX_SNAPSHOT_ATTEMPTS,
                        exception.getMessage()
                );
            }
        }
        throw new IllegalStateException(label + " 동기화를 완료하지 못했습니다.");
    }

    private int expectedPageCount(int totalCount) {
        int pageSize = Math.max(1, appProperties.publicData().syncPageSize());
        return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
    }

    private void recordFailure(LocalDateTime startedAt, String message) {
        try {
            syncWriter.recordHistory(DataSyncStatus.FAILED, startedAt, message);
        } catch (RuntimeException historyException) {
            log.error("동기화 실패 이력 저장에도 실패했습니다: {}", historyException.getMessage());
        }
    }

    private static final class SnapshotChangedException extends RuntimeException {

        private SnapshotChangedException(String message) {
            super(message);
        }
    }
}
