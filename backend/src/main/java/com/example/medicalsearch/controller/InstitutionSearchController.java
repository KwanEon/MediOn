package com.example.medicalsearch.controller;

import com.example.medicalsearch.dto.EmergencyBedAvailabilityRequest;
import com.example.medicalsearch.dto.EmergencyBedAvailabilityResponse;
import com.example.medicalsearch.dto.NearbyInstitutionResponse;
import com.example.medicalsearch.entity.HospitalDepartment;
import com.example.medicalsearch.entity.InstitutionType;
import com.example.medicalsearch.entity.OperatingScheduleFilter;
import com.example.medicalsearch.service.InstitutionSearchService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/institutions")
public class InstitutionSearchController {

    private final InstitutionSearchService institutionSearchService;

    public InstitutionSearchController(InstitutionSearchService institutionSearchService) {
        this.institutionSearchService = institutionSearchService;
    }

    @GetMapping("/nearby")
    public NearbyInstitutionResponse searchNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false) Integer radiusMeters,
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "HOSPITAL,PHARMACY,EMERGENCY_ROOM") List<InstitutionType> types,
            @RequestParam(required = false) HospitalDepartment hospitalDepartment,
            @RequestParam(defaultValue = "ALL") OperatingScheduleFilter operatingSchedule,
            @RequestParam(defaultValue = "true") boolean openNowOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "30") @Min(1) @Max(500) int size
    ) {
        return institutionSearchService.searchNearby(
                lat,
                lng,
                radiusMeters,
                keyword,
                types,
                hospitalDepartment,
                operatingSchedule,
                openNowOnly,
                page,
                size
        );
    }

    @PostMapping("/emergency-beds")
    public EmergencyBedAvailabilityResponse getEmergencyBedAvailability(
            @Valid @RequestBody EmergencyBedAvailabilityRequest request
    ) {
        return institutionSearchService.getEmergencyBedAvailability(request.institutionIds());
    }
}
