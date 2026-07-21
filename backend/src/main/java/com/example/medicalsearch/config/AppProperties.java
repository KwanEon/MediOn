package com.example.medicalsearch.config;

import java.net.URI;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        ZoneId timeZone,
        Cors cors,
        PublicData publicData,
        OpenStreetMap openStreetMap,
        NaverMaps naverMaps
) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record PublicData(
            boolean enabled,
            String serviceKey,
            boolean hospitalLocationEnabled,
            boolean emergencyMedicalEnabled,
            URI hospitalUrl,
            URI hospitalListUrl,
            URI pharmacyUrl,
            URI pharmacyListUrl,
            URI emergencyUrl,
            URI emergencyListUrl,
            URI emergencyAvailabilityUrl,
            Duration timeout,
            Duration cacheTtl,
            Duration emergencyAvailabilityCacheTtl,
            Duration operatingHoursCacheTtl,
            int maxOperatingHoursRows,
            int maxResults
    ) {
    }

    public record OpenStreetMap(
            boolean enabled,
            URI overpassUrl,
            List<URI> fallbackUrls,
            Duration timeout
    ) {
    }

    public record NaverMaps(
            URI geocodingUrl,
            String apiKeyId,
            String apiKey,
            Duration timeout
    ) {
    }
}
