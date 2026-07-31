package com.example.medicalsearch.service;

import com.example.medicalsearch.config.AppProperties;
import com.example.medicalsearch.dto.DeveloperDashboardResponse;
import com.example.medicalsearch.dto.DeveloperDashboardResponse.ExternalService;
import com.example.medicalsearch.dto.DeveloperDashboardResponse.Metrics;
import com.example.medicalsearch.dto.DeveloperDashboardResponse.SyncHistory;
import com.example.medicalsearch.dto.DeveloperDashboardResponse.SyncState;
import com.example.medicalsearch.dto.DeveloperUserPageResponse;
import com.example.medicalsearch.dto.DeveloperUserResponse;
import com.example.medicalsearch.dto.PageResponse;
import com.example.medicalsearch.dto.SyncTriggerResponse;
import com.example.medicalsearch.entity.DataSyncHistory;
import com.example.medicalsearch.entity.InstitutionType;
import com.example.medicalsearch.entity.UserRole;
import com.example.medicalsearch.repository.AppUserRepository;
import com.example.medicalsearch.repository.DataSyncHistoryRepository;
import com.example.medicalsearch.repository.MedicalInstitutionRepository;
import com.example.medicalsearch.repository.UserFavoriteRepository;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeveloperService {

    private static final int MAX_USER_PAGE_SIZE = 100;
    private static final Duration STALE_DATA_THRESHOLD = Duration.ofHours(48);

    private final AppUserRepository userRepository;
    private final UserFavoriteRepository favoriteRepository;
    private final MedicalInstitutionRepository institutionRepository;
    private final DataSyncHistoryRepository syncHistoryRepository;
    private final MedicalInstitutionDataSyncService hospitalSyncService;
    private final PharmacyDataSyncService pharmacySyncService;
    private final AppProperties appProperties;
    private final String applicationVersion;

    public DeveloperService(
            AppUserRepository userRepository,
            UserFavoriteRepository favoriteRepository,
            MedicalInstitutionRepository institutionRepository,
            DataSyncHistoryRepository syncHistoryRepository,
            MedicalInstitutionDataSyncService hospitalSyncService,
            PharmacyDataSyncService pharmacySyncService,
            AppProperties appProperties,
            @Value("${spring.application.version:0.0.1}") String applicationVersion
    ) {
        this.userRepository = userRepository;
        this.favoriteRepository = favoriteRepository;
        this.institutionRepository = institutionRepository;
        this.syncHistoryRepository = syncHistoryRepository;
        this.hospitalSyncService = hospitalSyncService;
        this.pharmacySyncService = pharmacySyncService;
        this.appProperties = appProperties;
        this.applicationVersion = applicationVersion;
    }

    @Transactional(readOnly = true)
    public DeveloperDashboardResponse getDashboard() {
        ZoneId timeZone = appProperties.timeZone();
        LocalDateTime now = LocalDateTime.now(timeZone);
        LocalDateTime staleBefore = now.minus(STALE_DATA_THRESHOLD);
        long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();

        Metrics metrics = new Metrics(
                userRepository.count(),
                userRepository.countByRole(UserRole.DEVELOPER),
                userRepository.countByCreatedAtAfter(now.minusDays(7)),
                institutionRepository.countByActiveTrue(),
                institutionRepository.countByActiveTrueAndType(InstitutionType.HOSPITAL),
                institutionRepository.countByActiveTrueAndType(InstitutionType.PHARMACY),
                institutionRepository.countByActiveTrueAndEmergencyRoomAvailableTrue(),
                institutionRepository.countByActiveFalse(),
                institutionRepository.countByActiveTrueAndLastSyncedAtBefore(staleBefore),
                institutionRepository.findLatestSyncedAt().orElse(null)
        );

        boolean publicDataEnabled = appProperties.publicData().enabled();
        boolean publicDataConfigured = publicDataEnabled
                && hasText(appProperties.publicData().serviceKey());
        boolean geocodingConfigured = hasText(appProperties.naverMaps().apiKeyId())
                && hasText(appProperties.naverMaps().apiKey());
        List<ExternalService> externalServices = List.of(
                externalService(
                        "institution-data",
                        "의료기관 공공데이터",
                        publicDataEnabled,
                        publicDataConfigured,
                        "병원·약국 기본 정보와 진료과목 동기화"
                ),
                externalService(
                        "emergency-beds",
                        "응급실 병상 정보",
                        publicDataEnabled,
                        publicDataConfigured,
                        "실시간 응급실 가용 병상 조회"
                ),
                externalService(
                        "geocoding",
                        "네이버 주소 검색",
                        true,
                        geocodingConfigured,
                        "회원 주소 검색과 좌표 변환"
                )
        );

        List<SyncHistory> recentSyncs = syncHistoryRepository
                .findTop12ByOrderBySyncedAtDescIdDesc()
                .stream()
                .map(this::toSyncHistory)
                .toList();

        return new DeveloperDashboardResponse(
                Instant.now(),
                Instant.ofEpochMilli(ManagementFactory.getRuntimeMXBean().getStartTime()),
                Math.max(0, uptimeMillis / 1000),
                "OPERATIONAL",
                applicationVersion,
                metrics,
                new SyncState(
                        publicDataEnabled,
                        hospitalSyncService.isSynchronizationRunning(),
                        pharmacySyncService.isSynchronizationRunning()
                ),
                externalServices,
                recentSyncs
        );
    }

    @Transactional(readOnly = true)
    public DeveloperUserPageResponse getUsers(String query, int page, int size) {
        String normalizedQuery = query == null ? "" : query.trim();
        int normalizedPage = Math.max(0, page);
        int normalizedSize = Math.max(1, Math.min(size, MAX_USER_PAGE_SIZE));
        PageRequest pageable = PageRequest.of(
                normalizedPage,
                normalizedSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<com.example.medicalsearch.entity.AppUser> users =
                userRepository.searchForDeveloper(normalizedQuery, pageable);
        List<DeveloperUserResponse> items = users.getContent().stream()
                .map(user -> DeveloperUserResponse.from(
                        user,
                        favoriteRepository.countByUserId(user.getId())
                ))
                .toList();
        return new DeveloperUserPageResponse(
                items,
                PageResponse.of(users.getNumber(), users.getSize(), users.getTotalElements())
        );
    }

    public SyncTriggerResponse triggerSynchronization(String requestedTarget) {
        String target = requestedTarget == null ? "" : requestedTarget.trim().toLowerCase();
        return switch (target) {
            case "hospitals" -> triggerHospitalSync();
            case "pharmacies" -> triggerPharmacySync();
            default -> throw new IllegalArgumentException("지원하지 않는 동기화 대상입니다.");
        };
    }

    private SyncTriggerResponse triggerHospitalSync() {
        boolean accepted = hospitalSyncService.requestSynchronization();
        String message = accepted
                ? "병원·진료과목 동기화를 시작했습니다."
                : unavailableSyncMessage(hospitalSyncService.isSynchronizationRunning());
        return new SyncTriggerResponse(accepted, "hospitals", message);
    }

    private SyncTriggerResponse triggerPharmacySync() {
        boolean accepted = pharmacySyncService.requestSynchronization();
        String message = accepted
                ? "약국 동기화를 시작했습니다."
                : unavailableSyncMessage(pharmacySyncService.isSynchronizationRunning());
        return new SyncTriggerResponse(accepted, "pharmacies", message);
    }

    private String unavailableSyncMessage(boolean running) {
        if (!appProperties.publicData().enabled()) {
            return "공공데이터 연동이 비활성화되어 있습니다.";
        }
        return running ? "이미 동기화가 진행 중입니다." : "동기화를 시작할 수 없습니다.";
    }

    private ExternalService externalService(
            String key,
            String name,
            boolean enabled,
            boolean configured,
            String description
    ) {
        String status = !enabled ? "DISABLED" : configured ? "READY" : "CONFIG_REQUIRED";
        return new ExternalService(key, name, status, description);
    }

    private SyncHistory toSyncHistory(DataSyncHistory history) {
        return new SyncHistory(
                history.getId(),
                history.getSourceName(),
                history.getTargetType(),
                history.getStatus().name(),
                history.getSyncedAt(),
                history.getMessage()
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
