package com.example.medicalsearch.client;

import com.example.medicalsearch.config.AppProperties;
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
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NaverMapsGeocodingClient {

    private final AppProperties.NaverMaps properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public NaverMapsGeocodingClient(ObjectMapper objectMapper, AppProperties appProperties) {
        this.properties = appProperties.naverMaps();
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public GeocodedAddress geocode(String address) {
        return search(address, 1).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "입력한 주소를 지도에서 찾을 수 없습니다. 도로명과 건물번호를 확인해 주세요."
                ));
    }

    public List<GeocodedAddress> search(String address, int count) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("검색할 주소를 입력해 주세요.");
        }
        if (properties.apiKeyId() == null || properties.apiKeyId().isBlank()
                || properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new AddressGeocodingException(
                    "네이버 지도 Geocoding API 인증키가 설정되지 않았습니다."
            );
        }

        String separator = properties.geocodingUrl().toString().contains("?") ? "&" : "?";
        String query = "query=" + URLEncoder.encode(address.trim(), StandardCharsets.UTF_8)
                + "&language=kor&page=1&count=" + Math.max(1, Math.min(count, 10));
        URI requestUri = URI.create(properties.geocodingUrl() + separator + query);
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(properties.timeout())
                .header("Accept", "application/json")
                .header("x-ncp-apigw-api-key-id", properties.apiKeyId().trim())
                .header("x-ncp-apigw-api-key", properties.apiKey().trim())
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AddressGeocodingException(
                        "네이버 지도 Geocoding API가 HTTP " + response.statusCode() + "을 반환했습니다."
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!"OK".equalsIgnoreCase(root.path("status").asText())) {
                throw new AddressGeocodingException(
                        root.path("errorMessage").asText("주소 좌표 변환에 실패했습니다.")
                );
            }

            JsonNode addresses = root.path("addresses");
            if (!addresses.isArray() || addresses.isEmpty()) {
                return List.of();
            }

            List<GeocodedAddress> results = new ArrayList<>();
            for (JsonNode result : addresses) {
                String roadAddress = result.path("roadAddress").asText(null);
                String jibunAddress = result.path("jibunAddress").asText(null);
                try {
                    results.add(new GeocodedAddress(
                            firstNonBlank(roadAddress, jibunAddress, address.trim()),
                            roadAddress,
                            jibunAddress,
                            new BigDecimal(result.path("y").asText()),
                            new BigDecimal(result.path("x").asText())
                    ));
                } catch (NumberFormatException ignored) {
                    // 좌표가 없는 비정상 검색 결과만 제외합니다.
                }
            }
            return List.copyOf(results);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AddressGeocodingException("주소 좌표 조회가 중단되었습니다.", exception);
        } catch (IOException | NumberFormatException exception) {
            throw new AddressGeocodingException("주소 좌표 응답을 처리할 수 없습니다.", exception);
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    public record GeocodedAddress(
            String address,
            String roadAddress,
            String jibunAddress,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }
}
