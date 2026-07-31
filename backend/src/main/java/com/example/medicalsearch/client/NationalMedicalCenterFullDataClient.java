package com.example.medicalsearch.client;

import com.example.medicalsearch.config.AppProperties;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
public class NationalMedicalCenterFullDataClient {

    private static final Logger log = LogManager.getLogger(NationalMedicalCenterFullDataClient.class);
    private static final Pattern ENCODED_CHARACTER_PATTERN = Pattern.compile("%[0-9a-fA-F]{2}");
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);
    private static final int MINUTES_PER_DAY = 24 * 60;
    private static final int MAX_EXTENDED_HOUR = 47;
    private static final int MAX_REQUEST_ATTEMPTS = 8;
    private static final long MIN_REQUEST_INTERVAL_MILLIS = 1_000L;
    private static final long INITIAL_RETRY_DELAY_MILLIS = 2_000L;
    private static final long MAX_RETRY_DELAY_MILLIS = 60_000L;

    private final AppProperties.PublicData properties;
    private final HttpClient httpClient;
    private long nextRequestAtNanos;

    public NationalMedicalCenterFullDataClient(AppProperties appProperties) {
        this.properties = appProperties.publicData();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    public FullDataPage fetchFullDataPage(int pageNumber) {
        return fetchFullDataPage(
                properties.hospitalFullDataUrl(),
                pageNumber,
                "병·의원 FullData"
        );
    }

    public FullDataPage fetchPharmacyFullDataPage(int pageNumber) {
        return fetchFullDataPage(
                properties.pharmacyFullDataUrl(),
                pageNumber,
                "약국 FullData"
        );
    }

    private FullDataPage fetchFullDataPage(URI endpoint, int pageNumber, String label) {
        Document document = request(buildPagedUri(
                endpoint,
                pageNumber,
                normalizedPageSize(),
                null
        ), label);
        NodeList itemNodes = document.getElementsByTagName("item");
        List<FullDataInstitution> institutions = new ArrayList<>(itemNodes.getLength());
        for (int index = 0; index < itemNodes.getLength(); index++) {
            institutions.add(parseFullDataInstitution(itemNodes.item(index)));
        }
        return new FullDataPage(
                List.copyOf(institutions),
                parseRequiredTotalCount(documentText(document, "totalCount"))
        );
    }

    public DepartmentPage fetchDepartmentPage(
            String departmentCode,
            int pageNumber
    ) {
        Document document = request(buildPagedUri(
                properties.hospitalDepartmentListUrl(),
                pageNumber,
                normalizedPageSize(),
                departmentCode
        ), "병·의원 진료과목 " + departmentCode);
        NodeList itemNodes = document.getElementsByTagName("item");
        Set<String> hpids = new LinkedHashSet<>();
        for (int index = 0; index < itemNodes.getLength(); index++) {
            String hpid = childText(itemNodes.item(index), "hpid");
            if (hpid == null) {
                throw new PublicDataClientException(
                        "병·의원 진료과목 응답에 필수 HPID가 없습니다."
                );
            }
            hpids.add(hpid);
        }
        return new DepartmentPage(
                List.copyOf(hpids),
                parseRequiredTotalCount(documentText(document, "totalCount")),
                itemNodes.getLength()
        );
    }

    private FullDataInstitution parseFullDataInstitution(Node itemNode) {
        String hpid = requiredChildText(itemNode, "hpid", "기관 ID");
        String name = requiredChildText(itemNode, "dutyName", "기관명");
        BigDecimal latitude = optionalDecimal(itemNode, "wgs84Lat", "위도");
        BigDecimal longitude = optionalDecimal(itemNode, "wgs84Lon", "경도");

        Map<DayOfWeek, DailyOperatingHours> operatingHours = new EnumMap<>(DayOfWeek.class);
        boolean nightService = false;
        boolean twentyFourHours = false;
        for (int dayIndex = 1; dayIndex <= 7; dayIndex++) {
            ParsedOperatingHours parsedHours = parseOperatingHours(itemNode, dayIndex);
            operatingHours.put(DayOfWeek.of(dayIndex), parsedHours.hours());
            nightService |= parsedHours.nightService();
            twentyFourHours |= parsedHours.twentyFourHours();
        }
        ParsedOperatingHours holidayHours = parseOperatingHours(itemNode, 8);
        nightService |= holidayHours.nightService();
        twentyFourHours |= holidayHours.twentyFourHours();

        return new FullDataInstitution(
                hpid,
                name,
                childText(itemNode, "dutyDivNam", "dutyDivName"),
                "1".equals(childText(itemNode, "dutyEryn")),
                childText(itemNode, "dutyTel1"),
                childText(itemNode, "dutyAddr"),
                latitude,
                longitude,
                Map.copyOf(operatingHours),
                nightService,
                twentyFourHours,
                !operatingHours.get(DayOfWeek.SATURDAY).closed(),
                !operatingHours.get(DayOfWeek.SUNDAY).closed(),
                !holidayHours.hours().closed()
        );
    }

    private ParsedOperatingHours parseOperatingHours(Node itemNode, int dayIndex) {
        Integer openMinutes = parseTimeMinutes(childText(itemNode, "dutyTime" + dayIndex + "s"));
        Integer closeMinutes = parseTimeMinutes(childText(
                itemNode,
                "dutyTime" + dayIndex + "c",
                "dutyTime" + dayIndex + "e"
        ));
        if (openMinutes == null || closeMinutes == null) {
            return new ParsedOperatingHours(
                    new DailyOperatingHours(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT, true),
                    false,
                    false
            );
        }

        boolean twentyFourHours = openMinutes == 0
                && (closeMinutes == 0 || closeMinutes == MINUTES_PER_DAY);
        boolean nightService = twentyFourHours
                || closeMinutes >= 20 * 60
                || closeMinutes <= openMinutes;
        return new ParsedOperatingHours(
                new DailyOperatingHours(
                        toLocalTime(openMinutes),
                        twentyFourHours ? END_OF_DAY : toLocalTime(closeMinutes),
                        false
                ),
                nightService,
                twentyFourHours
        );
    }

    private LocalTime toLocalTime(int minutes) {
        if (minutes == MINUTES_PER_DAY) {
            return END_OF_DAY;
        }
        int normalizedMinutes = minutes % MINUTES_PER_DAY;
        return LocalTime.of(normalizedMinutes / 60, normalizedMinutes % 60);
    }

    private Integer parseTimeMinutes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() == 3) {
            digits = "0" + digits;
        } else if (digits.length() > 4) {
            digits = digits.substring(0, 4);
        }
        if (digits.length() != 4) {
            return unknownOperatingTime(value);
        }
        int hour = Integer.parseInt(digits.substring(0, 2));
        int minute = Integer.parseInt(digits.substring(2, 4));
        if (hour > MAX_EXTENDED_HOUR || minute > 59) {
            return unknownOperatingTime(value);
        }
        return hour * 60 + minute;
    }

    private Integer unknownOperatingTime(String value) {
        log.warn("공공데이터의 잘못된 진료시간 값을 운영시간 미상으로 처리합니다: {}", value);
        return null;
    }

    private URI buildPagedUri(
            URI endpoint,
            int pageNumber,
            int pageSize,
            String departmentCode
    ) {
        String separator = endpoint.toString().contains("?") ? "&" : "?";
        StringBuilder query = new StringBuilder("serviceKey=")
                .append(encodedServiceKey())
                .append("&pageNo=").append(pageNumber)
                .append("&numOfRows=").append(pageSize);
        if (departmentCode != null) {
            query.append("&QD=").append(encodeQueryValue(departmentCode))
                    .append("&ORD=NAME");
        }
        return URI.create(endpoint + separator + query);
    }

    private synchronized Document request(URI requestUri, String label) {
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(properties.timeout())
                .header("Accept", "application/xml, text/xml")
                .header("User-Agent", "medion-full-data-sync/1.0")
                .GET()
                .build();

        for (int attempt = 1; attempt <= MAX_REQUEST_ATTEMPTS; attempt++) {
            waitForRequestSlot(label);
            HttpResponse<byte[]> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new PublicDataClientException(label + " API 조회가 중단되었습니다.", exception);
            } catch (IOException exception) {
                if (attempt == MAX_REQUEST_ATTEMPTS) {
                    throw new PublicDataClientException(label + " API에 연결할 수 없습니다.", exception);
                }
                waitBeforeRetry(label, attempt, retryDelayMillis(attempt), "연결 오류");
                continue;
            }

            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                if (isRetryableStatus(statusCode) && attempt < MAX_REQUEST_ATTEMPTS) {
                    waitBeforeRetry(
                            label,
                            attempt,
                            retryDelayMillis(response, attempt),
                            "HTTP " + statusCode
                    );
                    continue;
                }
                if (statusCode == 429) {
                    throw new PublicDataRateLimitException(
                            label + " API 요청 한도를 초과했습니다."
                    );
                }
                throw new PublicDataClientException(
                        label + " API가 HTTP " + statusCode + "을 반환했습니다."
                );
            }

            Document document = parseXml(response.body());
            if (isRateLimitResponse(document)) {
                if (attempt < MAX_REQUEST_ATTEMPTS) {
                    waitBeforeRetry(
                            label,
                            attempt,
                            retryDelayMillis(attempt),
                            "요청 한도 초과 응답"
                    );
                    continue;
                }
                throw new PublicDataRateLimitException(
                        label + " API 요청 한도를 초과했습니다."
                );
            }
            verifySuccessfulResponse(document, label);
            return document;
        }

        throw new PublicDataClientException(label + " API 재시도 횟수를 초과했습니다.");
    }

    private void waitForRequestSlot(String label) {
        long waitNanos = nextRequestAtNanos - System.nanoTime();
        if (waitNanos > 0) {
            sleep(label, waitNanos, "호출 간격 대기");
        }
        nextRequestAtNanos = System.nanoTime()
                + TimeUnit.MILLISECONDS.toNanos(MIN_REQUEST_INTERVAL_MILLIS);
    }

    private void waitBeforeRetry(
            String label,
            int completedAttempt,
            long delayMillis,
            String reason
    ) {
        log.warn(
                "{} API 요청 실패({}). {}/{}회 시도 후 {}초 뒤 재시도합니다.",
                label,
                reason,
                completedAttempt,
                MAX_REQUEST_ATTEMPTS,
                Duration.ofMillis(delayMillis).toSeconds()
        );
        sleep(label, TimeUnit.MILLISECONDS.toNanos(delayMillis), "재시도 대기");
    }

    private void sleep(String label, long waitNanos, String reason) {
        try {
            TimeUnit.NANOSECONDS.sleep(waitNanos);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublicDataClientException(
                    label + " API " + reason + "가 중단되었습니다.",
                    exception
            );
        }
    }

    private boolean isRetryableStatus(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private boolean isRateLimitResponse(Document document) {
        String resultCode = documentText(document, "resultCode");
        if ("22".equals(resultCode)) {
            return true;
        }
        String resultMessage = firstNonBlank(
                documentText(document, "resultMsg"),
                documentText(document, "returnAuthMsg"),
                documentText(document, "errMsg")
        );
        return resultMessage != null
                && resultMessage.toUpperCase(Locale.ROOT)
                        .contains("LIMITED_NUMBER_OF_SERVICE_REQUESTS");
    }

    private long retryDelayMillis(HttpResponse<?> response, int attempt) {
        return response.headers().firstValue("Retry-After")
                .flatMap(this::parseRetryAfterSeconds)
                .map(seconds -> Math.min(
                        seconds,
                        MAX_RETRY_DELAY_MILLIS / 1_000L
                ) * 1_000L)
                .orElseGet(() -> retryDelayMillis(attempt));
    }

    private Optional<Long> parseRetryAfterSeconds(String value) {
        try {
            return Optional.of(Math.max(1L, Long.parseLong(value.trim())));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private long retryDelayMillis(int attempt) {
        long multiplier = 1L << Math.min(attempt - 1, 10);
        return Math.min(INITIAL_RETRY_DELAY_MILLIS * multiplier, MAX_RETRY_DELAY_MILLIS);
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
            throw new PublicDataClientException("공공데이터 API 응답을 해석할 수 없습니다.", exception);
        }
    }

    private void verifySuccessfulResponse(Document document, String label) {
        String resultCode = documentText(document, "resultCode");
        if (resultCode != null && !"00".equals(resultCode) && !"0000".equals(resultCode)) {
            throw new PublicDataClientException(
                    label + " API 오류(" + resultCode + "): "
                            + firstNonBlank(documentText(document, "resultMsg"), "알 수 없는 오류")
            );
        }
        String gatewayCode = documentText(document, "returnReasonCode");
        if (gatewayCode != null && !"00".equals(gatewayCode) && !"0".equals(gatewayCode)) {
            throw new PublicDataClientException(
                    label + " API 인증 오류(" + gatewayCode + "): "
                            + firstNonBlank(
                                    documentText(document, "returnAuthMsg"),
                                    documentText(document, "errMsg"),
                                    "인증 오류"
                            )
            );
        }
    }

    private String requiredChildText(Node node, String tagName, String label) {
        String value = childText(node, tagName);
        if (value == null) {
            throw new PublicDataClientException("병·의원 FullData 응답에 필수 " + label + "이 없습니다.");
        }
        return value;
    }

    private BigDecimal optionalDecimal(Node node, String tagName, String label) {
        String value = childText(node, tagName);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException exception) {
            throw new PublicDataClientException("병·의원 FullData의 " + label + " 값이 잘못되었습니다.", exception);
        }
    }

    private String childText(Node parent, String... tagNames) {
        for (String tagName : tagNames) {
            NodeList children = parent.getChildNodes();
            for (int index = 0; index < children.getLength(); index++) {
                Node child = children.item(index);
                if (child.getNodeType() == Node.ELEMENT_NODE
                        && tagName.equalsIgnoreCase(child.getNodeName())) {
                    String value = child.getTextContent();
                    if (value != null && !value.isBlank()) {
                        return value.trim();
                    }
                }
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

    private int parseRequiredTotalCount(String value) {
        if (value == null) {
            throw new PublicDataClientException("공공데이터 API 응답에 필수 전체 건수가 없습니다.");
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException exception) {
            throw new PublicDataClientException("공공데이터 API 전체 건수 값이 잘못되었습니다.", exception);
        }
    }

    private String encodedServiceKey() {
        String configuredServiceKey = properties.serviceKey();
        if (configuredServiceKey == null || configuredServiceKey.isBlank()) {
            throw new PublicDataClientException(
                    "공공데이터 인증키가 없습니다. application-secret.properties의 "
                            + "app.public-data.service-key를 설정해 주세요."
            );
        }
        String serviceKey = configuredServiceKey.trim();
        if (ENCODED_CHARACTER_PATTERN.matcher(serviceKey).find()) {
            serviceKey = URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);
        }
        return encodeQueryValue(serviceKey);
    }

    private int normalizedPageSize() {
        return Math.max(1, properties.syncPageSize());
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

    public record FullDataPage(List<FullDataInstitution> items, int totalCount) {
    }

    public record DepartmentPage(List<String> hpids, int totalCount, int itemCount) {

        public DepartmentPage(List<String> hpids, int totalCount) {
            this(hpids, totalCount, hpids.size());
        }
    }

    public record FullDataInstitution(
            String hpid,
            String name,
            String institutionKindName,
            boolean emergencyRoomAvailable,
            String phoneNumber,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            Map<DayOfWeek, DailyOperatingHours> operatingHours,
            boolean nightService,
            boolean twentyFourHours,
            boolean saturdayService,
            boolean sundayService,
            boolean holidayService
    ) {
    }

    public record DailyOperatingHours(
            LocalTime openTime,
            LocalTime closeTime,
            boolean closed
    ) {
    }

    private record ParsedOperatingHours(
            DailyOperatingHours hours,
            boolean nightService,
            boolean twentyFourHours
    ) {
    }
}
