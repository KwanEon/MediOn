package com.example.medicalsearch.service;

import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient;
import com.example.medicalsearch.client.NationalMedicalCenterFullDataClient.FullDataPage;
import com.example.medicalsearch.config.AppProperties;
import com.example.medicalsearch.entity.DataSyncStatus;
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
public class PharmacyDataSyncService {

    private static final Logger log = LogManager.getLogger(PharmacyDataSyncService.class);
    private static final int MAX_SNAPSHOT_ATTEMPTS = 3;

    private final NationalMedicalCenterFullDataClient fullDataClient;
    private final MedicalInstitutionSyncWriter syncWriter;
    private final AppProperties appProperties;
    private final AtomicBoolean synchronizationRunning = new AtomicBoolean(false);
    private final AtomicBoolean synchronizationRequested = new AtomicBoolean(false);

    public PharmacyDataSyncService(
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
            log.info("PUBLIC_DATA_ENABLED=false여서 약국 FullData 동기화를 건너뜁니다.");
            return;
        }
        boolean pharmacyTableEmpty = syncWriter.isPharmacyTableEmpty();
        boolean initialSyncIncomplete = pharmacyTableEmpty
                || !syncWriter.hasCompletedPharmacyFullDataSync();
        if (!initialSyncIncomplete && !appProperties.publicData().syncOnStartup()) {
            return;
        }
        LocalDate today = LocalDate.now(appProperties.timeZone());
        if (!initialSyncIncomplete
                && syncWriter.hasSuccessfulPharmacySyncSince(today.atStartOfDay())) {
            return;
        }
        if (pharmacyTableEmpty) {
            log.info("활성 약국 데이터가 없어 약국 FullData 초기 동기화를 시작합니다.");
        }
        CompletableFuture.runAsync(this::synchronize);
    }

    @Scheduled(
            cron = "${app.public-data.sync-cron:0 0 3 * * *}",
            zone = "${app.time-zone:Asia/Seoul}"
    )
    public void synchronize() {
        if (!fullDataClient.isEnabled()) {
            log.info("PUBLIC_DATA_ENABLED=false여서 약국 FullData 동기화를 건너뜁니다.");
            return;
        }
        if (!synchronizationRunning.compareAndSet(false, true)) {
            log.info("약국 FullData 동기화가 이미 실행 중입니다.");
            return;
        }

        String syncRunId = UUID.randomUUID().toString();
        LocalDateTime startedAt = LocalDateTime.now(appProperties.timeZone());
        try {
            int pharmacyCount = synchronizeFullData(syncRunId, startedAt);
            int inactiveCount = syncWriter.deactivateMissingPharmacies(syncRunId, startedAt);
            String message = "약국 upsert=" + pharmacyCount
                    + ", 미수신 약국 비활성화=" + inactiveCount;
            syncWriter.recordPharmacyHistory(DataSyncStatus.SUCCESS, startedAt, message);
            log.info("약국 FullData 동기화 완료: {}", message);
        } catch (RuntimeException exception) {
            String message = "동기화 실패: " + exception.getMessage();
            try {
                syncWriter.recordPharmacyHistory(DataSyncStatus.FAILED, startedAt, message);
            } catch (RuntimeException historyException) {
                log.error("약국 동기화 실패 이력 저장에도 실패했습니다: {}", historyException.getMessage());
            }
            log.error("약국 FullData 동기화 실패. 기존 활성 상태는 유지합니다.", exception);
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
            FullDataPage page = fullDataClient.fetchPharmacyFullDataPage(pageNumber);
            if (pageNumber == 1 && (page.totalCount() <= 0 || page.items().isEmpty())) {
                throw new IllegalStateException(
                        "약국 FullData 전체 건수가 0이어서 기존 약국을 비활성화하지 않습니다."
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
                throw new IllegalStateException("약국 FullData 페이지가 전체 건수 전에 비었습니다.");
            }
            int uniqueCountBeforePage = receivedHpids.size();
            syncWriter.upsertPharmacies(page.items(), syncRunId, syncedAt);
            receivedItemCount += page.items().size();
            page.items().forEach(pharmacy -> receivedHpids.add(pharmacy.hpid()));
            log.info(
                    "약국 FullData 페이지 저장: page={}, 페이지 건수={}, "
                            + "누적 수신={}/{}, 고유 HPID={}",
                    pageNumber,
                    page.items().size(),
                    receivedItemCount,
                    expectedTotalCount,
                    receivedHpids.size()
            );
            if (!page.items().isEmpty() && receivedHpids.size() == uniqueCountBeforePage) {
                throw new IllegalStateException("약국 FullData에 동일한 페이지가 반복되었습니다.");
            }
            if (receivedItemCount > expectedTotalCount) {
                throw new IllegalStateException(
                        "약국 FullData 수신 행 수가 전체 건수를 초과했습니다: "
                                + receivedItemCount + "/" + expectedTotalCount
                );
            }
            pageNumber++;
            if (pageNumber > maxPageCount && receivedItemCount < expectedTotalCount) {
                throw new IllegalStateException("약국 FullData에 중복 페이지가 반복되었습니다.");
            }
        } while (receivedItemCount < expectedTotalCount);

        int duplicateHpidCount = receivedItemCount - receivedHpids.size();
        if (duplicateHpidCount > 0) {
            log.warn(
                    "약국 FullData에 중복 HPID가 있어 고유 기관 기준으로 저장했습니다: "
                            + "수신 행={}, 고유 HPID={}, 중복 행={}",
                    receivedItemCount,
                    receivedHpids.size(),
                    duplicateHpidCount
            );
        }

        return receivedHpids.size();
    }

    private int synchronizeStableSnapshot(IntSupplier synchronization) {
        for (int attempt = 1; attempt <= MAX_SNAPSHOT_ATTEMPTS; attempt++) {
            try {
                return synchronization.getAsInt();
            } catch (SnapshotChangedException exception) {
                if (attempt == MAX_SNAPSHOT_ATTEMPTS) {
                    throw new IllegalStateException(
                            "약국 FullData 전체 건수가 계속 변경되어 "
                                    + MAX_SNAPSHOT_ATTEMPTS
                                    + "회 수집 시도 후 중단했습니다. 기존 활성 데이터는 유지합니다.",
                            exception
                    );
                }
                log.warn(
                        "약국 FullData 전체 건수가 수집 도중 변경되어 첫 페이지부터 다시 시도합니다: "
                                + "시도={}/{}, {}",
                        attempt + 1,
                        MAX_SNAPSHOT_ATTEMPTS,
                        exception.getMessage()
                );
            }
        }
        throw new IllegalStateException("약국 FullData 동기화를 완료하지 못했습니다.");
    }

    private int expectedPageCount(int totalCount) {
        int pageSize = Math.max(1, appProperties.publicData().syncPageSize());
        return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
    }

    private static final class SnapshotChangedException extends RuntimeException {

        private SnapshotChangedException(String message) {
            super(message);
        }
    }
}
