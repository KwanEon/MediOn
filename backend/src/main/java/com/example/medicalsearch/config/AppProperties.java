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
        NaverMaps naverMaps
) {

    public record Cors(List<String> allowedOrigins) {
    }

    public record PublicData(
            boolean enabled,
            String serviceKey,
            URI hospitalFullDataUrl,
            URI hospitalDepartmentListUrl,
            URI pharmacyFullDataUrl,
            Duration timeout,
            int syncPageSize,
            boolean syncOnStartup
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
