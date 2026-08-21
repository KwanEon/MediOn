package com.example.medicalsearch.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenStreetMapStationSearchClient {

    private static final String USER_AGENT = "MediOn/1.0 (https://github.com/KwanEon/MediOn)";

    private final ObjectMapper objectMapper;
    private final URI searchUrl;
    private final Duration timeout;
    private final HttpClient httpClient;

    public OpenStreetMapStationSearchClient(
            ObjectMapper objectMapper,
            @Value("${app.openstreetmap.station-search-url:https://nominatim.openstreetmap.org/search}")
            URI searchUrl,
            @Value("${app.openstreetmap.timeout:10s}") Duration timeout
    ) {
        this.objectMapper = objectMapper;
        this.searchUrl = searchUrl;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<StationLocation> search(String stationName, int count) {
        if (stationName == null || stationName.isBlank()) {
            return List.of();
        }

        String normalizedQuery = normalizeStationQuery(stationName);
        String separator = searchUrl.toString().contains("?") ? "&" : "?";
        String query = "q=" + URLEncoder.encode(normalizedQuery, StandardCharsets.UTF_8)
                + "&format=jsonv2&addressdetails=1&namedetails=1&countrycodes=kr"
                + "&limit=" + Math.max(1, Math.min(count, 10));
        URI requestUri = URI.create(searchUrl + separator + query);
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(timeout)
                .header("Accept", "application/json")
                .header("Accept-Language", "ko")
                .header("User-Agent", USER_AGENT)
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AddressGeocodingException(
                        "역 검색 API가 HTTP " + response.statusCode() + "을 반환했습니다."
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray()) {
                throw new AddressGeocodingException("역 검색 응답을 처리할 수 없습니다.");
            }

            List<StationLocation> results = new ArrayList<>();
            for (JsonNode result : root) {
                if (!isStation(result)) {
                    continue;
                }

                String displayName = result.path("display_name").asText("").trim();
                String name = result.path("name").asText("").trim();
                if (name.isBlank() && !displayName.isBlank()) {
                    name = displayName.split(",", 2)[0].trim();
                }
                if (name.isBlank()) {
                    continue;
                }

                try {
                    results.add(new StationLocation(
                            name,
                            displayName,
                            new BigDecimal(result.path("lat").asText()),
                            new BigDecimal(result.path("lon").asText())
                    ));
                } catch (NumberFormatException ignored) {
                    // 좌표가 없는 검색 결과만 제외합니다.
                }
            }
            return List.copyOf(results);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AddressGeocodingException("역 위치 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new AddressGeocodingException("역 검색 응답을 처리할 수 없습니다.", exception);
        }
    }

    private String normalizeStationQuery(String query) {
        String normalized = query.trim();
        return normalized.contains("역") ? normalized : normalized + "역";
    }

    private boolean isStation(JsonNode result) {
        String type = result.path("type").asText("").toLowerCase(Locale.ROOT);
        String addressType = result.path("addresstype").asText("").toLowerCase(Locale.ROOT);
        String name = result.path("name").asText("").trim();

        return isStationType(type)
                || isStationType(addressType)
                || name.endsWith("역");
    }

    private boolean isStationType(String value) {
        return "station".equals(value)
                || "subway_station".equals(value)
                || "subway_entrance".equals(value)
                || "halt".equals(value)
                || "stop".equals(value);
    }

    public record StationLocation(
            String name,
            String displayName,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }
}
