package com.example.medicalsearch.client;

import com.example.medicalsearch.config.AppProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component
public class EmergencyBedAvailabilityClient {

    private static final Logger log = LogManager.getLogger(EmergencyBedAvailabilityClient.class);
    private static final Pattern ENCODED_CHARACTER_PATTERN = Pattern.compile("%[0-9a-fA-F]{2}");
    private static final long CACHE_TTL_NANOS = Duration.ofMinutes(1).toNanos();
    private static final int PAGE_SIZE = 1000;

    private final AppProperties.PublicData properties;
    private final HttpClient httpClient;
    private final Map<Region, CachedAvailability> regionCache = new ConcurrentHashMap<>();

    public EmergencyBedAvailabilityClient(AppProperties appProperties) {
        this.properties = appProperties.publicData();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public Map<String, Integer> fetchAvailableBeds(List<EmergencyInstitution> institutions) {
        if (!properties.enabled() || institutions.isEmpty()) {
            return Map.of();
        }

        Set<String> targetHpids = new LinkedHashSet<>();
        Set<Region> regions = new LinkedHashSet<>();
        for (EmergencyInstitution institution : institutions) {
            targetHpids.add(institution.hpid());
            Region region = Region.fromAddress(institution.roadAddress());
            if (region != null) {
                regions.add(region);
            }
        }

        Map<String, Integer> result = new HashMap<>();
        for (Region region : regions) {
            Map<String, Integer> regionAvailability = getRegionAvailability(region);
            for (String hpid : targetHpids) {
                Integer availableBeds = regionAvailability.get(hpid);
                if (availableBeds != null) {
                    result.put(hpid, availableBeds);
                }
            }
        }
        return Map.copyOf(result);
    }

    private Map<String, Integer> getRegionAvailability(Region region) {
        long now = System.nanoTime();
        CachedAvailability cached = regionCache.get(region);
        if (cached != null && cached.expiresAtNanos() > now) {
            return cached.availableBeds();
        }

        try {
            Map<String, Integer> availableBeds = requestRegion(region);
            regionCache.put(
                    region,
                    new CachedAvailability(availableBeds, now + CACHE_TTL_NANOS)
            );
            return availableBeds;
        } catch (PublicDataClientException exception) {
            log.warn(
                    "응급실 실시간 가용 병상 정보를 불러오지 못했습니다: region={} {}, reason={}",
                    region.stage1(),
                    region.stage2(),
                    exception.getMessage()
            );
            Map<String, Integer> emptyResult = Map.of();
            regionCache.put(
                    region,
                    new CachedAvailability(emptyResult, now + CACHE_TTL_NANOS)
            );
            return emptyResult;
        }
    }

    private Map<String, Integer> requestRegion(Region region) {
        URI requestUri = buildRequestUri(region);
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(properties.timeout())
                .header("Accept", "application/xml, text/xml")
                .header("User-Agent", "medion-emergency-bed-lookup/1.0")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PublicDataClientException(
                        "응급실 병상 API가 HTTP " + response.statusCode() + "을 반환했습니다."
                );
            }
            Document document = parseXml(response.body());
            verifySuccessfulResponse(document);
            return parseAvailableBeds(document);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublicDataClientException("응급실 병상 API 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new PublicDataClientException("응급실 병상 API에 연결할 수 없습니다.", exception);
        }
    }

    private URI buildRequestUri(Region region) {
        String endpoint = properties.emergencyBedAvailabilityUrl().toString();
        String separator = endpoint.contains("?") ? "&" : "?";
        return URI.create(endpoint + separator
                + "serviceKey=" + encodedServiceKey()
                + "&STAGE1=" + encodeQueryValue(region.stage1())
                + "&STAGE2=" + encodeQueryValue(region.stage2())
                + "&pageNo=1"
                + "&numOfRows=" + PAGE_SIZE);
    }

    private Map<String, Integer> parseAvailableBeds(Document document) {
        Map<String, Integer> availableBeds = new HashMap<>();
        NodeList itemNodes = document.getElementsByTagName("item");
        for (int index = 0; index < itemNodes.getLength(); index++) {
            Node item = itemNodes.item(index);
            String hpid = childText(item, "hpid");
            String hvec = childText(item, "hvec");
            if (hpid == null || hvec == null) {
                continue;
            }
            try {
                availableBeds.put(hpid, Math.max(0, Integer.parseInt(hvec)));
            } catch (NumberFormatException exception) {
                log.warn("응급실 병상 API의 hvec 값이 숫자가 아닙니다: hpid={}, hvec={}", hpid, hvec);
            }
        }
        return Map.copyOf(availableBeds);
    }

    private Document parseXml(byte[] responseBody) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(responseBody));
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new PublicDataClientException("응급실 병상 API 응답을 해석할 수 없습니다.", exception);
        }
    }

    private void verifySuccessfulResponse(Document document) {
        String resultCode = documentText(document, "resultCode");
        if (resultCode != null && !"00".equals(resultCode) && !"0000".equals(resultCode)) {
            throw new PublicDataClientException(
                    "응급실 병상 API 오류(" + resultCode + "): "
                            + firstNonBlank(
                                    documentText(document, "resultMsg"),
                                    documentText(document, "resultMag"),
                                    "알 수 없는 오류"
                            )
            );
        }
        String gatewayCode = documentText(document, "returnReasonCode");
        if (gatewayCode != null && !"00".equals(gatewayCode) && !"0".equals(gatewayCode)) {
            throw new PublicDataClientException(
                    "응급실 병상 API 인증 오류(" + gatewayCode + "): "
                            + firstNonBlank(
                                    documentText(document, "returnAuthMsg"),
                                    documentText(document, "errMsg"),
                                    "인증 오류"
                            )
            );
        }
    }

    private String childText(Node parent, String tagName) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && tagName.equalsIgnoreCase(child.getNodeName())) {
                String value = child.getTextContent();
                return value == null || value.isBlank() ? null : value.trim();
            }
        }
        return null;
    }

    private String documentText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        String value = nodes.item(0).getTextContent();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String encodedServiceKey() {
        String configuredServiceKey = properties.serviceKey();
        if (configuredServiceKey == null || configuredServiceKey.isBlank()) {
            throw new PublicDataClientException(
                    "공공데이터 인증키가 없습니다. DATA_GO_KR_SERVICE_KEY를 설정해 주세요."
            );
        }
        String serviceKey = configuredServiceKey.trim();
        if (ENCODED_CHARACTER_PATTERN.matcher(serviceKey).find()) {
            serviceKey = URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);
        }
        return encodeQueryValue(serviceKey);
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    public record EmergencyInstitution(String hpid, String roadAddress) {
    }

    private record Region(String stage1, String stage2) {

        private static Region fromAddress(String address) {
            if (address == null || address.isBlank()) {
                return null;
            }
            String[] parts = address.trim().split("\\s+");
            if (parts.length == 0) {
                return null;
            }
            if ("세종특별자치시".equals(parts[0])) {
                return new Region(parts[0], "");
            }
            if (parts.length < 2) {
                return null;
            }
            return new Region(parts[0], parts[1]);
        }
    }

    private record CachedAvailability(
            Map<String, Integer> availableBeds,
            long expiresAtNanos
    ) {
    }
}
