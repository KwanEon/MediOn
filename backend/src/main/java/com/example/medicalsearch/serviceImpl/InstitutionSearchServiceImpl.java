package com.example.medicalsearch.serviceImpl;

import com.example.medicalsearch.client.NationalMedicalCenterInstitutionClient;
import com.example.medicalsearch.client.OpenStreetMapInstitutionClient;
import com.example.medicalsearch.client.PublicDataClientException;
import com.example.medicalsearch.config.AppProperties;
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
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InstitutionSearchServiceImpl implements InstitutionSearchService {

    private static final Logger log = LoggerFactory.getLogger(InstitutionSearchServiceImpl.class);
    private static final int DEFAULT_RADIUS_METERS = 3000;
    private static final int MAX_RADIUS_METERS = 10000;
    private static final int MAX_SEARCH_RESULTS = 100;
    private static final List<InstitutionType> ALL_INSTITUTION_TYPES = List.of(
            InstitutionType.HOSPITAL,
            InstitutionType.PHARMACY,
            InstitutionType.EMERGENCY_ROOM
    );

    private final MedicalInstitutionRepository medicalInstitutionRepository;
    private final NationalMedicalCenterInstitutionClient nationalMedicalCenterInstitutionClient;
    private final OpenStreetMapInstitutionClient openStreetMapInstitutionClient;
    private final AppProperties appProperties;

    public InstitutionSearchServiceImpl(
            MedicalInstitutionRepository medicalInstitutionRepository,
            NationalMedicalCenterInstitutionClient nationalMedicalCenterInstitutionClient,
            OpenStreetMapInstitutionClient openStreetMapInstitutionClient,
            AppProperties appProperties
    ) {
        this.medicalInstitutionRepository = medicalInstitutionRepository;
        this.nationalMedicalCenterInstitutionClient = nationalMedicalCenterInstitutionClient;
        this.openStreetMapInstitutionClient = openStreetMapInstitutionClient;
        this.appProperties = appProperties;
    }

    @Override
    public NearbyInstitutionResponse searchOpenNearby(
            double lat,
            double lng,
            Integer radiusMeters,
            List<InstitutionType> types,
            HospitalDepartment hospitalDepartment,
            OperatingScheduleFilter operatingSchedule,
            int page,
            int size
    ) {
        validateCoordinate(lat, lng);
        int normalizedRadiusMeters = normalizeRadius(radiusMeters);
        int normalizedSize = Math.min(size, MAX_SEARCH_RESULTS);

        ZonedDateTime requestedAt = ZonedDateTime.now(appProperties.timeZone());
        List<InstitutionType> normalizedTypes = normalizeTypes(types);
        HospitalDepartment effectiveHospitalDepartment = normalizedTypes.contains(InstitutionType.HOSPITAL)
                ? hospitalDepartment
                : null;

        PublicDataClientException publicDataFailure = null;
        if (nationalMedicalCenterInstitutionClient.isEnabled()) {
            try {
                List<NearbyInstitutionItemResponse> actualInstitutions =
                        nationalMedicalCenterInstitutionClient.searchNearby(
                                lat,
                                lng,
                                normalizedRadiusMeters,
                                ALL_INSTITUTION_TYPES,
                                requestedAt
                        );
                return pageActualInstitutions(
                        filterByOperatingSchedule(
                                filterByHospitalDepartment(
                                        filterByInstitutionTypes(actualInstitutions, normalizedTypes),
                                        effectiveHospitalDepartment
                                ),
                                operatingSchedule
                        ),
                        requestedAt,
                        normalizedRadiusMeters,
                        page,
                        normalizedSize
                );
            } catch (PublicDataClientException exception) {
                publicDataFailure = exception;
                log.warn("공공데이터 의료기관 조회 실패, OpenStreetMap 조회로 전환합니다: {}", exception.getMessage());
            }
        }

        if (openStreetMapInstitutionClient.isEnabled()) {
            List<NearbyInstitutionItemResponse> actualInstitutions = openStreetMapInstitutionClient.searchNearby(
                    lat,
                    lng,
                    normalizedRadiusMeters,
                    ALL_INSTITUTION_TYPES,
                    requestedAt
            );
            return pageActualInstitutions(
                    filterByOperatingSchedule(
                            filterByHospitalDepartment(
                                    filterByInstitutionTypes(actualInstitutions, normalizedTypes),
                                    effectiveHospitalDepartment
                            ),
                            operatingSchedule
                    ),
                    requestedAt,
                    normalizedRadiusMeters,
                    page,
                    normalizedSize
            );
        }

        if (publicDataFailure != null) {
            throw publicDataFailure;
        }

        String dayOfWeek = requestedAt.getDayOfWeek().name();
        LocalTime currentTime = requestedAt.toLocalTime();
        List<String> typeNames = normalizedTypes.stream()
                .map(Enum::name)
                .toList();

        Page<NearbyInstitutionRow> nearbyPage = medicalInstitutionRepository.findOpenNearby(
                lat,
                lng,
                normalizedRadiusMeters,
                typeNames,
                dayOfWeek,
                currentTime,
                PageRequest.of(page, normalizedSize)
        );

        List<NearbyInstitutionItemResponse> items = nearbyPage.getContent().stream()
                .map(NearbyInstitutionItemResponse::from)
                .filter(item -> effectiveHospitalDepartment == null
                        || item.medicalDepartments().contains(
                                effectiveHospitalDepartment.getDisplayName()
                        ))
                .filter(item -> operatingSchedule.matches(item.operatingSchedules()))
                .toList();
        long uncappedTotalElements = effectiveHospitalDepartment == null
                && operatingSchedule == OperatingScheduleFilter.ALL
                ? nearbyPage.getTotalElements()
                : items.size();
        long totalElements = Math.min(uncappedTotalElements, MAX_SEARCH_RESULTS);
        LocalDateTime lastSyncedAt = medicalInstitutionRepository.findLatestSyncedAt().orElse(null);

        return new NearbyInstitutionResponse(
                requestedAt,
                normalizedRadiusMeters,
                lastSyncedAt,
                items,
                PageResponse.of(page, normalizedSize, totalElements)
        );
    }

    private List<NearbyInstitutionItemResponse> filterByInstitutionTypes(
            List<NearbyInstitutionItemResponse> institutions,
            List<InstitutionType> requestedTypes
    ) {
        return institutions.stream()
                .filter(institution -> requestedTypes.contains(institution.type()))
                .toList();
    }

    private List<NearbyInstitutionItemResponse> filterByHospitalDepartment(
            List<NearbyInstitutionItemResponse> institutions,
            HospitalDepartment hospitalDepartment
    ) {
        if (hospitalDepartment == null) {
            return institutions;
        }
        return institutions.stream()
                .filter(institution -> institution.type() == InstitutionType.HOSPITAL)
                .filter(institution -> institution.medicalDepartments().contains(
                        hospitalDepartment.getDisplayName()
                ))
                .toList();
    }

    private List<NearbyInstitutionItemResponse> filterByOperatingSchedule(
            List<NearbyInstitutionItemResponse> institutions,
            OperatingScheduleFilter operatingSchedule
    ) {
        if (operatingSchedule == OperatingScheduleFilter.ALL) {
            return institutions;
        }
        return institutions.stream()
                .filter(institution -> operatingSchedule.matches(institution.operatingSchedules()))
                .toList();
    }

    private NearbyInstitutionResponse pageActualInstitutions(
            List<NearbyInstitutionItemResponse> institutions,
            ZonedDateTime requestedAt,
            int radiusMeters,
            int page,
            int size
    ) {
        List<NearbyInstitutionItemResponse> orderedInstitutions = institutions.stream()
                .sorted(Comparator.comparingLong(NearbyInstitutionItemResponse::distanceMeters))
                .limit(MAX_SEARCH_RESULTS)
                .toList();
        int totalElements = orderedInstitutions.size();
        long requestedOffset = (long) page * size;
        int fromIndex = (int) Math.min(requestedOffset, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<NearbyInstitutionItemResponse> items = List.copyOf(
                orderedInstitutions.subList(fromIndex, toIndex)
        );

        return new NearbyInstitutionResponse(
                requestedAt,
                radiusMeters,
                requestedAt.toLocalDateTime(),
                items,
                PageResponse.of(page, size, totalElements)
        );
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

    private List<InstitutionType> normalizeTypes(List<InstitutionType> types) {
        if (types == null || types.isEmpty()) {
            return List.of(InstitutionType.HOSPITAL, InstitutionType.PHARMACY);
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
