package com.example.medicalsearch.dto;

import com.example.medicalsearch.entity.InstitutionType;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

public record NearbyInstitutionResponse(
        ZonedDateTime requestedAt,
        int radiusMeters,
        LocalDateTime lastSyncedAt,
        Map<InstitutionType, Long> typeCounts,
        List<NearbyInstitutionItemResponse> items,
        PageResponse page
) {
}
