package com.example.medicalsearch.dto;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

public record NearbyInstitutionResponse(
        ZonedDateTime requestedAt,
        int radiusMeters,
        LocalDateTime lastSyncedAt,
        List<NearbyInstitutionItemResponse> items,
        PageResponse page
) {
}
