package com.example.medicalsearch.serviceImpl;

import com.example.medicalsearch.client.EmergencyBedAvailabilityClient;
import com.example.medicalsearch.client.EmergencyBedAvailabilityClient.EmergencyInstitution;
import com.example.medicalsearch.config.AppProperties;
import com.example.medicalsearch.dto.EmergencyBedAvailabilityResponse;
import com.example.medicalsearch.dto.NearbyInstitutionItemResponse;
import com.example.medicalsearch.dto.NearbyInstitutionResponse;
import com.example.medicalsearch.dto.PageResponse;
import com.example.medicalsearch.entity.HospitalDepartment;
import com.example.medicalsearch.entity.InstitutionType;
import com.example.medicalsearch.entity.OperatingScheduleFilter;
import com.example.medicalsearch.repository.MedicalInstitutionRepository;
import com.example.medicalsearch.repository.NearbyInstitutionRow;
import com.example.medicalsearch.service.InstitutionSearchService;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InstitutionSearchServiceImpl implements InstitutionSearchService {

    private static final int DEFAULT_RADIUS_METERS = 3000;
    private static final int MAX_RADIUS_METERS = 10000;
    private static final int MAX_PAGE_SIZE = 500;

    private final MedicalInstitutionRepository medicalInstitutionRepository;
    private final AppProperties appProperties;
    private final EmergencyBedAvailabilityClient emergencyBedAvailabilityClient;

    public InstitutionSearchServiceImpl(
            MedicalInstitutionRepository medicalInstitutionRepository,
            AppProperties appProperties,
            EmergencyBedAvailabilityClient emergencyBedAvailabilityClient
    ) {
        this.medicalInstitutionRepository = medicalInstitutionRepository;
        this.appProperties = appProperties;
        this.emergencyBedAvailabilityClient = emergencyBedAvailabilityClient;
    }

    @Override
    public NearbyInstitutionResponse searchNearby(
            double lat,
            double lng,
            Integer radiusMeters,
            List<InstitutionType> types,
            HospitalDepartment hospitalDepartment,
            OperatingScheduleFilter operatingSchedule,
            boolean openNowOnly,
            int page,
            int size
    ) {
        validateCoordinate(lat, lng);
        int normalizedRadiusMeters = normalizeRadius(radiusMeters);
        int normalizedSize = Math.min(size, MAX_PAGE_SIZE);

        ZonedDateTime requestedAt = ZonedDateTime.now(appProperties.timeZone());
        List<InstitutionType> normalizedTypes = normalizeTypes(types);
        String departmentCode = normalizedTypes.contains(InstitutionType.HOSPITAL)
                && hospitalDepartment != null
                ? departmentFilterCode(hospitalDepartment)
                : null;

        Page<NearbyInstitutionRow> nearbyPage = medicalInstitutionRepository.findNearby(
                lat,
                lng,
                normalizedRadiusMeters,
                normalizedTypes.contains(InstitutionType.HOSPITAL),
                normalizedTypes.contains(InstitutionType.PHARMACY),
                normalizedTypes.contains(InstitutionType.EMERGENCY_ROOM),
                requestedAt.getDayOfWeek().name(),
                requestedAt.toLocalTime(),
                departmentCode,
                operatingSchedule.name(),
                openNowOnly,
                PageRequest.of(page, normalizedSize)
        );

        List<NearbyInstitutionItemResponse> items = nearbyPage.getContent().stream()
                .map(row -> NearbyInstitutionItemResponse.from(row, null))
                .toList();
        LocalDateTime lastSyncedAt = medicalInstitutionRepository.findLatestSyncedAt().orElse(null);

        return new NearbyInstitutionResponse(
                requestedAt,
                normalizedRadiusMeters,
                lastSyncedAt,
                items,
                PageResponse.of(page, normalizedSize, nearbyPage.getTotalElements())
        );
    }

    @Override
    public EmergencyBedAvailabilityResponse getEmergencyBedAvailability(List<Long> institutionIds) {
        List<Long> normalizedInstitutionIds = institutionIds.stream()
                .distinct()
                .toList();
        if (normalizedInstitutionIds.isEmpty()) {
            return new EmergencyBedAvailabilityResponse(Map.of());
        }
        if (normalizedInstitutionIds.size() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "institutionIds must contain at most " + MAX_PAGE_SIZE + " values."
            );
        }

        var emergencyInstitutions = medicalInstitutionRepository
                .findActiveEmergencyInstitutionsByIdIn(normalizedInstitutionIds);
        Map<String, Integer> availableBedsByHpid = emergencyBedAvailabilityClient
                .fetchAvailableBeds(emergencyInstitutions.stream()
                        .filter(row -> row.getHpid() != null)
                        .map(row -> new EmergencyInstitution(row.getHpid(), row.getRoadAddress()))
                        .toList());

        Map<Long, Integer> availableBedsByInstitutionId = new LinkedHashMap<>();
        for (var institution : emergencyInstitutions) {
            Integer availableBeds = availableBedsByHpid.get(institution.getHpid());
            if (availableBeds != null) {
                availableBedsByInstitutionId.put(institution.getId(), availableBeds);
            }
        }
        return new EmergencyBedAvailabilityResponse(Map.copyOf(availableBedsByInstitutionId));
    }

    private int normalizeRadius(Integer radiusMeters) {
        if (radiusMeters == null) {
            return DEFAULT_RADIUS_METERS;
        }
        if (radiusMeters <= 0 || radiusMeters > MAX_RADIUS_METERS) {
            throw new IllegalArgumentException("radiusMeters must be between 1 and " + MAX_RADIUS_METERS + ".");
        }
        return radiusMeters;
    }

    private String departmentFilterCode(HospitalDepartment hospitalDepartment) {
        if (hospitalDepartment == HospitalDepartment.KOREAN_CLINIC) {
            return HospitalDepartment.KOREAN_CLINIC.name();
        }
        return hospitalDepartment.getPublicDataCode();
    }

    private List<InstitutionType> normalizeTypes(List<InstitutionType> types) {
        if (types == null || types.isEmpty()) {
            return List.of(
                    InstitutionType.HOSPITAL,
                    InstitutionType.PHARMACY,
                    InstitutionType.EMERGENCY_ROOM
            );
        }
        return types.stream().distinct().toList();
    }

    private void validateCoordinate(double lat, double lng) {
        if (lat < -90 || lat > 90) {
            throw new IllegalArgumentException("lat must be between -90 and 90.");
        }
        if (lng < -180 || lng > 180) {
            throw new IllegalArgumentException("lng must be between -180 and 180.");
        }
    }
}
