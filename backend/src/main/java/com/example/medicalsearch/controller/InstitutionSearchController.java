package com.example.medicalsearch.controller;

import com.example.medicalsearch.dto.NearbyInstitutionResponse;
import com.example.medicalsearch.entity.HospitalDepartment;
import com.example.medicalsearch.entity.InstitutionType;
import com.example.medicalsearch.entity.OperatingScheduleFilter;
import com.example.medicalsearch.service.InstitutionSearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
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
            @RequestParam(defaultValue = "HOSPITAL,PHARMACY,EMERGENCY_ROOM") List<InstitutionType> types,
            @RequestParam(required = false) HospitalDepartment hospitalDepartment,
            @RequestParam(defaultValue = "ALL") OperatingScheduleFilter operatingSchedule,
            @RequestParam(defaultValue = "true") boolean openNowOnly,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(500) int size
    ) {
        return institutionSearchService.searchNearby(
                lat,
                lng,
                radiusMeters,
                types,
                hospitalDepartment,
                operatingSchedule,
                openNowOnly,
                page,
                size
        );
    }
}
