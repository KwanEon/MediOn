package com.example.medicalsearch.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record DeveloperDashboardResponse(
        Instant generatedAt,
        Instant serverStartedAt,
        long uptimeSeconds,
        String serviceStatus,
        String applicationVersion,
        Metrics metrics,
        SyncState syncState,
        List<ExternalService> externalServices,
        List<SyncHistory> recentSyncs
) {

    public record Metrics(
            long totalUsers,
            long developerUsers,
            long newUsersLast7Days,
            long activeInstitutions,
            long hospitals,
            long pharmacies,
            long emergencyRooms,
            long inactiveInstitutions,
            long staleInstitutions,
            LocalDateTime latestInstitutionSync
    ) {
    }

    public record SyncState(
            boolean publicDataEnabled,
            boolean hospitalSyncRunning,
            boolean pharmacySyncRunning
    ) {
    }

    public record ExternalService(
            String key,
            String name,
            String status,
            String description
    ) {
    }

    public record SyncHistory(
            Long id,
            String sourceName,
            String targetType,
            String status,
            LocalDateTime syncedAt,
            String message
    ) {
    }
}
