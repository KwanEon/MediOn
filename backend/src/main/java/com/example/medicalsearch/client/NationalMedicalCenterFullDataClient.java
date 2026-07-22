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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final AppProperties.PublicData properties;
    private final HttpClient httpClient;

    public NationalMedicalCenterFullDataClient(AppProperties appProperties) {
        this.properties = appProperties.publicData();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public boolean isEnabled() {
        return properties.enabled()
                && properties.serviceKey() != null
                && !properties.serviceKey().isBlank();
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
                parseRequiredTotalCount(documentText(document, "totalCount"))
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
                childText(itemNode, "dutyDiv"),
                childText(itemNode, "dutyDivNam", "dutyDivName"),
                childText(itemNode, "dutyEmcls"),
                childText(itemNode, "dutyEmclsName"),
                "1".equals(childText(itemNode, "dutyEryn")),
                childText(itemNode, "dutyTel1"),
                childText(itemNode, "dutyTel3"),
                childText(itemNode, "dutyAddr"),
                combinePostalCode(
                        childText(itemNode, "postCdn1"),
                        childText(itemNode, "postCdn2")
                ),
                childText(itemNode, "dutyEtc"),
                childText(itemNode, "dutyMapimg"),
                childText(itemNode, "dutyInf"),
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

    private Document request(URI requestUri, String label) {
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(properties.timeout())
                .header("Accept", "application/xml, text/xml")
                .header("User-Agent", "medion-full-data-sync/1.0")
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PublicDataClientException(
                        label + " API가 HTTP " + response.statusCode() + "을 반환했습니다."
                );
            }
            Document document = parseXml(response.body());
            verifySuccessfulResponse(document, label);
            return document;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublicDataClientException(label + " API 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new PublicDataClientException(label + " API에 연결할 수 없습니다.", exception);
        }
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

    private String combinePostalCode(String first, String second) {
        if (first == null) {
            return second;
        }
        if (second == null) {
            return first;
        }
        return first + second;
    }

    private String encodedServiceKey() {
        String serviceKey = properties.serviceKey().trim();
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

    public record DepartmentPage(List<String> hpids, int totalCount) {
    }

    public record FullDataInstitution(
            String hpid,
            String name,
            String institutionKindCode,
            String institutionKindName,
            String emergencyClassCode,
            String emergencyClassName,
            boolean emergencyRoomAvailable,
            String phoneNumber,
            String emergencyPhone,
            String roadAddress,
            String postalCode,
            String note,
            String mapDescription,
            String description,
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
