package com.example.medicalsearch.service;

import com.example.medicalsearch.dto.EmergencyBedAvailabilityResponse;
import com.example.medicalsearch.dto.NearbyInstitutionResponse;
import com.example.medicalsearch.entity.HospitalDepartment;
import com.example.medicalsearch.entity.InstitutionType;
import com.example.medicalsearch.entity.OperatingScheduleFilter;
import java.util.List;

public interface InstitutionSearchService {

    NearbyInstitutionResponse searchNearby(
            double lat,
            double lng,
            Integer radiusMeters,
            String keyword,
            List<InstitutionType> types,
            HospitalDepartment hospitalDepartment,
            OperatingScheduleFilter operatingSchedule,
            boolean openNowOnly,
            int page,
            int size
    );

    NearbyInstitutionResponse searchNearby(
            double lat,
            double lng,
            Integer radiusMeters,
            String keyword,
            List<InstitutionType> types,
            HospitalDepartment hospitalDepartment,
            OperatingScheduleFilter operatingSchedule,
            boolean openNowOnly,
            int page,
            int size,
            String favoriteUsername
    );

    EmergencyBedAvailabilityResponse getEmergencyBedAvailability(List<Long> institutionIds);
}
