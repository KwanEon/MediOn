package com.example.medicalsearch.client;

import com.example.medicalsearch.config.AppProperties;
import com.example.medicalsearch.dto.NearbyInstitutionItemResponse;
import com.example.medicalsearch.entity.InstitutionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class OpenStreetMapInstitutionClient {

    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile(
            "(\\d{1,2}):(\\d{2})\\s*-\\s*(\\d{1,2}):(\\d{2})"
    );
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final ObjectMapper objectMapper;
    private final AppProperties.OpenStreetMap properties;
    private final HttpClient httpClient;

    public OpenStreetMapInstitutionClient(ObjectMapper objectMapper, AppProperties appProperties) {
        this.objectMapper = objectMapper;
        this.properties = appProperties.openStreetMap();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public boolean isEnabled() {
        return properties.enabled();
    }

    public List<NearbyInstitutionItemResponse> searchNearby(
            double lat,
            double lng,
            int radiusMeters,
            List<InstitutionType> requestedTypes,
            ZonedDateTime requestedAt
    ) {
        String query = buildQuery(lat, lng, radiusMeters);
        String requestBody = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        List<String> failures = new ArrayList<>();

        for (URI endpoint : endpoints()) {
            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(properties.timeout())
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("Accept", "application/json")
                    .header("User-Agent", "medion-medical-search/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    failures.add(endpoint.getHost() + ": HTTP " + response.statusCode());
                    continue;
                }

                OverpassResponse overpassResponse = objectMapper.readValue(response.body(), OverpassResponse.class);
                if (overpassResponse.remark() != null && !overpassResponse.remark().isBlank()) {
                    failures.add(endpoint.getHost() + ": " + overpassResponse.remark());
                    continue;
                }
                return mapElements(overpassResponse.elements(), lat, lng, requestedTypes, requestedAt);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new OpenStreetMapClientException("OpenStreetMap 의료기관 조회가 중단되었습니다.", exception);
            } catch (IOException exception) {
                failures.add(endpoint.getHost() + ": " + exception.getClass().getSimpleName());
            }
        }

        throw new OpenStreetMapClientException(
                "모든 OpenStreetMap 공개 서버 조회에 실패했습니다. " + String.join("; ", failures)
        );
    }

    private List<URI> endpoints() {
        Set<URI> endpoints = new LinkedHashSet<>();
        endpoints.add(properties.overpassUrl());
        if (properties.fallbackUrls() != null) {
            endpoints.addAll(properties.fallbackUrls());
        }
        return List.copyOf(endpoints);
    }

    private String buildQuery(double lat, double lng, int radiusMeters) {
        long timeoutSeconds = Math.max(1, properties.timeout().toSeconds());
        return String.format(Locale.ROOT, """
                [out:json][timeout:%d];
                (
                  nwr(around:%d,%.7f,%.7f)["amenity"~"^(hospital|clinic|doctors|pharmacy)$"];
                  nwr(around:%d,%.7f,%.7f)["healthcare"~"^(hospital|clinic|doctor|doctors|pharmacy)$"];
                );
                out body center qt 500;
                """,
                timeoutSeconds,
                radiusMeters, lat, lng,
                radiusMeters, lat, lng
        );
    }

    private List<NearbyInstitutionItemResponse> mapElements(
            List<OverpassElement> elements,
            double searchLat,
            double searchLng,
            List<InstitutionType> requestedTypes,
            ZonedDateTime requestedAt
    ) {
        if (elements == null || elements.isEmpty()) {
            return List.of();
        }

        Set<InstitutionType> allowedTypes = Set.copyOf(requestedTypes);
        Map<Long, NearbyInstitutionItemResponse> institutionsById = new LinkedHashMap<>();

        for (OverpassElement element : elements) {
            Coordinates coordinates = coordinatesOf(element);
            Map<String, String> tags = element.tags() == null ? Map.of() : element.tags();
            String name = firstNonBlank(tags.get("name:ko"), tags.get("name"));
            if (coordinates == null || name == null) {
                continue;
            }
            InstitutionType type = resolveType(tags);
            if (!allowedTypes.contains(type)) {
                continue;
            }

            long id = externalId(element.type(), element.id());
            OperatingHours operatingHours = resolveOperatingHours(tags.get("opening_hours"), requestedAt);
            long distanceMeters = Math.round(distanceMeters(
                    searchLat,
                    searchLng,
                    coordinates.lat(),
                    coordinates.lng()
            ));

            institutionsById.putIfAbsent(id, new NearbyInstitutionItemResponse(
                    id,
                    type,
                    name,
                    resolveInstitutionKind(name, tags, type),
                    resolveMedicalDepartments(name, tags, type),
                    firstNonBlank(tags.get("contact:phone"), tags.get("phone")),
                    resolveAddress(tags),
                    BigDecimal.valueOf(coordinates.lat()),
                    BigDecimal.valueOf(coordinates.lng()),
                    distanceMeters,
                    operatingHours.open(),
                    operatingHours.known(),
                    operatingHours.openTime(),
                    operatingHours.closeTime(),
                    null,
                    Set.of(),
                    requestedAt.toLocalDateTime()
            ));
        }

        return institutionsById.values().stream()
                .sorted((first, second) -> Long.compare(first.distanceMeters(), second.distanceMeters()))
                .toList();
    }

    private Coordinates coordinatesOf(OverpassElement element) {
        if (element.lat() != null && element.lon() != null) {
            return new Coordinates(element.lat(), element.lon());
        }
        if (element.center() != null) {
            return new Coordinates(element.center().lat(), element.center().lon());
        }
        return null;
    }

    private InstitutionType resolveType(Map<String, String> tags) {
        String amenity = tags.get("amenity");
        String healthcare = tags.get("healthcare");
        if ("pharmacy".equals(amenity) || "pharmacy".equals(healthcare)) {
            return InstitutionType.PHARMACY;
        }
        if ("yes".equalsIgnoreCase(tags.get("emergency"))) {
            return InstitutionType.EMERGENCY_ROOM;
        }
        return InstitutionType.HOSPITAL;
    }

    private String resolveInstitutionKind(
            String name,
            Map<String, String> tags,
            InstitutionType type
    ) {
        if (type == InstitutionType.PHARMACY) {
            return null;
        }
        if (name.contains("상급종합병원")) {
            return "상급종합병원";
        }
        if (name.contains("종합병원")) {
            return "종합병원";
        }
        if (name.contains("치과병원")) {
            return "치과병원";
        }
        if (name.contains("한방병원")) {
            return "한방병원";
        }
        if (name.contains("한의원")) {
            return "한의원";
        }
        if (name.contains("치과")) {
            return "치과의원";
        }
        if (name.contains("의원")) {
            return "의원";
        }

        String category = firstNonBlank(tags.get("healthcare"), tags.get("amenity"));
        if ("clinic".equals(category) || "doctor".equals(category) || "doctors".equals(category)) {
            return "의원";
        }
        if (type == InstitutionType.EMERGENCY_ROOM) {
            return "응급의료기관";
        }
        return "병원";
    }

    private Set<String> resolveMedicalDepartments(
            String name,
            Map<String, String> tags,
            InstitutionType type
    ) {
        if (type != InstitutionType.HOSPITAL) {
            return Set.of();
        }
        Set<String> departments = new LinkedHashSet<>();
        addMedicalDepartments(departments, specialtyKindFromTags(tags));
        addMedicalDepartments(departments, specialtyKindFromName(name));
        return Set.copyOf(departments);
    }

    private void addMedicalDepartments(Set<String> departments, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        for (String department : value.split("\\s*·\\s*")) {
            if (!department.isBlank()) {
                departments.add(department);
            }
        }
    }

    private String specialtyKindFromName(String name) {
        String normalized = name.replaceAll("\\s+", "");
        Set<String> specialties = new LinkedHashSet<>();
        if (normalized.contains("이비인후과")) {
            specialties.add("이비인후과");
        }
        if (normalized.contains("소아청소년과")) {
            specialties.add("소아과");
        }
        if (normalized.contains("내과")) {
            specialties.add("내과");
        }
        if (normalized.contains("안과")) {
            specialties.add("안과");
        }
        if (normalized.contains("피부과")) {
            specialties.add("피부과");
        }
        if (normalized.contains("산부인과")) {
            specialties.add("산부인과");
        }
        if (normalized.contains("정형외과")) {
            specialties.add("정형외과");
        }
        if (normalized.contains("외과") && !normalized.contains("정형외과")) {
            specialties.add("외과");
        }
        return specialties.isEmpty() ? null : String.join(" · ", specialties);
    }

    private String specialtyKindFromTags(Map<String, String> tags) {
        String value = firstNonBlank(
                tags.get("healthcare:speciality"),
                tags.get("healthcare:specialty")
        );
        if (value == null) {
            return null;
        }

        String normalized = value.toLowerCase(Locale.ROOT);
        Set<String> specialties = new LinkedHashSet<>();
        if (normalized.contains("otolaryng") || normalized.contains("ear_nose_throat")) {
            specialties.add("이비인후과");
        }
        if (normalized.contains("pediatric") || normalized.contains("paediatric")) {
            specialties.add("소아과");
        }
        if (normalized.contains("internal")) {
            specialties.add("내과");
        }
        if (normalized.contains("ophthalmolog")) {
            specialties.add("안과");
        }
        if (normalized.contains("dermatolog")) {
            specialties.add("피부과");
        }
        if (normalized.contains("obstetric")
                || normalized.contains("gynecolog")
                || normalized.contains("gynaecolog")) {
            specialties.add("산부인과");
        }
        boolean orthopedics = normalized.contains("orthopedic")
                || normalized.contains("orthopaedic");
        if (orthopedics) {
            specialties.add("정형외과");
        }
        if ((normalized.contains("surgery") || normalized.contains("surgeon")) && !orthopedics) {
            specialties.add("외과");
        }
        if (normalized.contains("dentist") || normalized.contains("dentistry")) {
            specialties.add("치과의원");
        }
        if (normalized.contains("traditional") || normalized.contains("korean_medicine")) {
            specialties.add("한의원");
        }
        return specialties.isEmpty() ? null : String.join(" · ", specialties);
    }

    private String resolveAddress(Map<String, String> tags) {
        String fullAddress = firstNonBlank(tags.get("addr:full"), tags.get("contact:address"));
        if (fullAddress != null) {
            return fullAddress;
        }

        Set<String> parts = new LinkedHashSet<>();
        addIfPresent(parts, tags.get("addr:province"));
        addIfPresent(parts, tags.get("addr:city"));
        addIfPresent(parts, tags.get("addr:district"));
        addIfPresent(parts, tags.get("addr:suburb"));
        addIfPresent(parts, tags.get("addr:street"));
        addIfPresent(parts, tags.get("addr:housenumber"));
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private OperatingHours resolveOperatingHours(String value, ZonedDateTime requestedAt) {
        if (value == null || value.isBlank()) {
            return OperatingHours.unknown();
        }
        if ("24/7".equalsIgnoreCase(value.trim())) {
            return new OperatingHours(true, true, LocalTime.MIDNIGHT, LocalTime.MAX);
        }

        DayOfWeek dayOfWeek = requestedAt.getDayOfWeek();
        int currentMinutes = requestedAt.getHour() * 60 + requestedAt.getMinute();

        for (String rawSegment : value.split(";")) {
            String segment = rawSegment.trim();
            Matcher matcher = TIME_RANGE_PATTERN.matcher(segment);
            if (!matcher.find()) {
                String selector = segment.replaceAll("(?i)\\b(off|closed)\\b", "").trim();
                if (segment.matches("(?i).*\\b(off|closed)\\b.*") && appliesToDay(selector, dayOfWeek)) {
                    return new OperatingHours(true, false, null, null);
                }
                continue;
            }

            String daySelector = segment.substring(0, matcher.start()).trim();
            if (!appliesToDay(daySelector, dayOfWeek)) {
                continue;
            }

            List<MinuteRange> ranges = new ArrayList<>();
            do {
                MinuteRange range = parseRange(matcher);
                if (range != null) {
                    ranges.add(range);
                }
            } while (matcher.find());

            if (ranges.isEmpty()) {
                continue;
            }

            boolean open = ranges.stream().anyMatch((range) -> range.includes(currentMinutes));
            MinuteRange firstRange = ranges.get(0);
            MinuteRange lastRange = ranges.get(ranges.size() - 1);
            return new OperatingHours(
                    true,
                    open,
                    toLocalTime(firstRange.startMinutes()),
                    toLocalTime(lastRange.endMinutes())
            );
        }

        return OperatingHours.unknown();
    }

    private MinuteRange parseRange(Matcher matcher) {
        int startHour = Integer.parseInt(matcher.group(1));
        int startMinute = Integer.parseInt(matcher.group(2));
        int endHour = Integer.parseInt(matcher.group(3));
        int endMinute = Integer.parseInt(matcher.group(4));
        if (startHour > 23 || endHour > 24 || startMinute > 59 || endMinute > 59) {
            return null;
        }
        if (endHour == 24 && endMinute != 0) {
            return null;
        }
        return new MinuteRange(startHour * 60 + startMinute, endHour * 60 + endMinute);
    }

    private boolean appliesToDay(String selector, DayOfWeek dayOfWeek) {
        if (selector == null || selector.isBlank()) {
            return true;
        }

        int currentDay = dayOfWeek.getValue();
        String compactSelector = selector.replace(" ", "");
        for (String part : compactSelector.split(",")) {
            if (part.contains("-")) {
                String[] range = part.split("-", 2);
                int start = dayIndex(range[0]);
                int end = dayIndex(range[1]);
                if (start > 0 && end > 0 && isDayInRange(currentDay, start, end)) {
                    return true;
                }
            } else if (dayIndex(part) == currentDay) {
                return true;
            }
        }
        return false;
    }

    private boolean isDayInRange(int day, int start, int end) {
        return start <= end
                ? day >= start && day <= end
                : day >= start || day <= end;
    }

    private int dayIndex(String day) {
        return switch (day) {
            case "Mo" -> 1;
            case "Tu" -> 2;
            case "We" -> 3;
            case "Th" -> 4;
            case "Fr" -> 5;
            case "Sa" -> 6;
            case "Su" -> 7;
            default -> -1;
        };
    }

    private LocalTime toLocalTime(int minutes) {
        if (minutes >= 24 * 60) {
            return LocalTime.MAX;
        }
        return LocalTime.of(minutes / 60, minutes % 60);
    }

    private long externalId(String elementType, long osmId) {
        int typeCode = switch (elementType) {
            case "node" -> 1;
            case "way" -> 2;
            case "relation" -> 3;
            default -> 9;
        };
        return osmId * 10 + typeCode;
    }

    private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private void addIfPresent(Set<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value.trim());
        }
    }

    private record OverpassResponse(String remark, List<OverpassElement> elements) {
    }

    private record OverpassElement(
            String type,
            long id,
            Double lat,
            Double lon,
            OverpassCenter center,
            Map<String, String> tags
    ) {
    }

    private record OverpassCenter(double lat, double lon) {
    }

    private record Coordinates(double lat, double lng) {
    }

    private record OperatingHours(
            boolean known,
            boolean open,
            LocalTime openTime,
            LocalTime closeTime
    ) {
        private static OperatingHours unknown() {
            return new OperatingHours(false, false, null, null);
        }
    }

    private record MinuteRange(int startMinutes, int endMinutes) {
        private boolean includes(int currentMinutes) {
            if (startMinutes < endMinutes) {
                return currentMinutes >= startMinutes && currentMinutes < endMinutes;
            }
            if (startMinutes > endMinutes) {
                return currentMinutes >= startMinutes || currentMinutes < endMinutes;
            }
            return false;
        }
    }
}
