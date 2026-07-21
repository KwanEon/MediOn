package com.example.medicalsearch.client;

import com.example.medicalsearch.config.AppProperties;
import com.example.medicalsearch.dto.NearbyInstitutionItemResponse;
import com.example.medicalsearch.entity.HospitalDepartment;
import com.example.medicalsearch.entity.InstitutionType;
import com.example.medicalsearch.entity.OperatingScheduleFilter;
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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component
public class NationalMedicalCenterInstitutionClient {

    private static final Logger log = LoggerFactory.getLogger(NationalMedicalCenterInstitutionClient.class);
    private static final Pattern ENCODED_CHARACTER_PATTERN = Pattern.compile("%[0-9a-fA-F]{2}");
    private static final Pattern MEDICAL_DEPARTMENT_SEPARATOR = Pattern.compile("[,;/|·]+");
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private static final Duration STALE_CACHE_RETENTION = Duration.ofMinutes(30);
    private static final int MAX_CONCURRENT_DETAIL_REQUESTS = 3;
    private static final int DETAIL_PAGE_SIZE = 200;
    private static final int EMERGENCY_AVAILABILITY_PAGE_SIZE = 100;

    private final AppProperties.PublicData properties;
    private final OpenStreetMapInstitutionClient openStreetMapInstitutionClient;
    private final HttpClient httpClient;
    private final ConcurrentMap<SearchKey, CachedSearch> cache = new ConcurrentHashMap<>();
    private final ConcurrentMap<OperatingHoursSearchKey, CachedPublicInstitutionLookup> operatingHoursCache =
            new ConcurrentHashMap<>();
    private final ConcurrentMap<EmergencyAvailabilitySearchKey, CachedEmergencyAvailability>
            emergencyAvailabilityCache = new ConcurrentHashMap<>();

    public NationalMedicalCenterInstitutionClient(
            AppProperties appProperties,
            OpenStreetMapInstitutionClient openStreetMapInstitutionClient
    ) {
        this.properties = appProperties.publicData();
        this.openStreetMapInstitutionClient = openStreetMapInstitutionClient;
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

    public List<NearbyInstitutionItemResponse> searchNearby(
            double lat,
            double lng,
            int radiusMeters,
            List<InstitutionType> requestedTypes,
            ZonedDateTime requestedAt
    ) {
        SearchKey searchKey = SearchKey.of(
                lat,
                lng,
                radiusMeters,
                requestedTypes
        );
        CachedSearch cachedSearch = cache.get(searchKey);
        if (cachedSearch != null && cachedSearch.expiresAt().isAfter(Instant.now())) {
            return cachedSearch.items();
        }

        Set<InstitutionType> allowedTypes = Set.copyOf(requestedTypes);
        List<ProviderRequest> providerRequests = providerRequests(allowedTypes);
        Set<InstitutionType> knownFallbackTypes = unsupportedPublicDataTypes(allowedTypes);
        CompletableFuture<List<NearbyInstitutionItemResponse>> knownFallbackFuture =
                openStreetMapInstitutionClient.isEnabled() && !knownFallbackTypes.isEmpty()
                        ? CompletableFuture.supplyAsync(() -> openStreetMapInstitutionClient.searchNearby(
                                lat,
                                lng,
                                radiusMeters,
                                List.copyOf(knownFallbackTypes),
                                requestedAt
                        ))
                        : null;
        List<CompletableFuture<List<NearbyInstitutionItemResponse>>> futures = providerRequests.stream()
                .map(provider -> CompletableFuture.supplyAsync(() -> requestProvider(
                        provider,
                        lat,
                        lng,
                        radiusMeters,
                        allowedTypes,
                        requestedAt
                )))
                .toList();

        List<NearbyInstitutionItemResponse> combined = new ArrayList<>();
        List<String> failures = new ArrayList<>();
        Set<InstitutionType> failedPublicDataTypes = new LinkedHashSet<>();
        int successfulProviders = 0;

        for (int index = 0; index < futures.size(); index++) {
            try {
                List<NearbyInstitutionItemResponse> providerItems = futures.get(index).join();
                combined.addAll(providerItems);
                if (providerItems.isEmpty()) {
                    failedPublicDataTypes.addAll(coveredTypes(providerRequests.get(index), allowedTypes));
                } else {
                    successfulProviders++;
                }
            } catch (CompletionException exception) {
                Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                failures.add(providerRequests.get(index).label() + ": " + cause.getMessage());
                failedPublicDataTypes.addAll(coveredTypes(providerRequests.get(index), allowedTypes));
            }
        }

        boolean fallbackCompleted = false;
        if (knownFallbackFuture != null) {
            try {
                combined.addAll(knownFallbackFuture.join());
                fallbackCompleted = true;
            } catch (CompletionException exception) {
                Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                failures.add("병·의원 대체 조회: " + cause.getMessage());
            }
        }

        failedPublicDataTypes.removeAll(knownFallbackTypes);
        if (openStreetMapInstitutionClient.isEnabled() && !failedPublicDataTypes.isEmpty()) {
            try {
                combined.addAll(openStreetMapInstitutionClient.searchNearby(
                        lat,
                        lng,
                        radiusMeters,
                        List.copyOf(failedPublicDataTypes),
                        requestedAt
                ));
                fallbackCompleted = true;
            } catch (OpenStreetMapClientException exception) {
                failures.add("실패한 종류 대체 조회: " + exception.getMessage());
            }
        }

        if (!failures.isEmpty() && cachedSearch != null && !cachedSearch.items().isEmpty()) {
            log.warn("일부 조회 실패로 직전 의료기관 목록을 유지합니다: {}", String.join("; ", failures));
            return cachedSearch.items();
        }
        if (successfulProviders == 0 && !fallbackCompleted && !failures.isEmpty()) {
            throw new PublicDataClientException(
                    "공공데이터와 대체 의료기관 조회에 실패했습니다. " + String.join("; ", failures)
            );
        }
        if (!failures.isEmpty()) {
            log.warn("일부 공공데이터 의료기관 조회에 실패했습니다: {}", String.join("; ", failures));
        }

        Map<Long, NearbyInstitutionItemResponse> uniqueItems = new LinkedHashMap<>();
        combined.stream()
                .sorted(Comparator.comparingLong(NearbyInstitutionItemResponse::distanceMeters))
                .forEach(item -> uniqueItems.putIfAbsent(item.id(), item));
        List<NearbyInstitutionItemResponse> result = enrichEmergencyAvailability(
                List.copyOf(uniqueItems.values()),
                requestedAt
        );
        result = addMissingTypeFallback(
                result,
                allowedTypes,
                lat,
                lng,
                radiusMeters,
                requestedAt
        );

        if (!properties.cacheTtl().isNegative() && !properties.cacheTtl().isZero()) {
            cache.put(searchKey, new CachedSearch(result, Instant.now().plus(properties.cacheTtl())));
            removeExpiredCacheEntries();
        }
        return result;
    }

    private List<NearbyInstitutionItemResponse> addMissingTypeFallback(
            List<NearbyInstitutionItemResponse> institutions,
            Set<InstitutionType> requestedTypes,
            double lat,
            double lng,
            int radiusMeters,
            ZonedDateTime requestedAt
    ) {
        if (!openStreetMapInstitutionClient.isEnabled()) {
            return institutions;
        }

        Set<InstitutionType> missingTypes = new LinkedHashSet<>(requestedTypes);
        institutions.stream()
                .map(NearbyInstitutionItemResponse::type)
                .forEach(missingTypes::remove);
        if (missingTypes.isEmpty()) {
            return institutions;
        }

        try {
            List<NearbyInstitutionItemResponse> fallbackItems = openStreetMapInstitutionClient.searchNearby(
                    lat,
                    lng,
                    radiusMeters,
                    List.copyOf(missingTypes),
                    requestedAt
            );
            Map<Long, NearbyInstitutionItemResponse> combined = new LinkedHashMap<>();
            institutions.forEach(item -> combined.putIfAbsent(item.id(), item));
            fallbackItems.forEach(item -> combined.putIfAbsent(item.id(), item));
            return combined.values().stream()
                    .sorted(Comparator.comparingLong(NearbyInstitutionItemResponse::distanceMeters))
                    .toList();
        } catch (OpenStreetMapClientException exception) {
            log.warn("누락된 의료기관 종류({}) 대체 조회 실패: {}", missingTypes, exception.getMessage());
            return institutions;
        }
    }

    private List<ProviderRequest> providerRequests(Set<InstitutionType> requestedTypes) {
        List<ProviderRequest> requests = new ArrayList<>();
        if (properties.hospitalLocationEnabled()
                && requestedTypes.contains(InstitutionType.HOSPITAL)) {
            requests.add(new ProviderRequest(properties.hospitalUrl(), InstitutionType.HOSPITAL, "병·의원"));
        }
        if (requestedTypes.contains(InstitutionType.PHARMACY)) {
            requests.add(new ProviderRequest(properties.pharmacyUrl(), InstitutionType.PHARMACY, "약국"));
        }
        if (properties.emergencyMedicalEnabled()
                && requestedTypes.contains(InstitutionType.EMERGENCY_ROOM)) {
            requests.add(new ProviderRequest(
                    properties.emergencyUrl(),
                    InstitutionType.EMERGENCY_ROOM,
                    "응급의료기관"
            ));
        }
        return requests;
    }

    private Set<InstitutionType> unsupportedPublicDataTypes(Set<InstitutionType> requestedTypes) {
        Set<InstitutionType> unsupportedTypes = new LinkedHashSet<>();
        if (!properties.hospitalLocationEnabled()) {
            if (requestedTypes.contains(InstitutionType.HOSPITAL)) {
                unsupportedTypes.add(InstitutionType.HOSPITAL);
            }
        }
        if (!properties.emergencyMedicalEnabled()
                && requestedTypes.contains(InstitutionType.EMERGENCY_ROOM)) {
            unsupportedTypes.add(InstitutionType.EMERGENCY_ROOM);
        }
        return unsupportedTypes;
    }

    private Set<InstitutionType> coveredTypes(
            ProviderRequest provider,
            Set<InstitutionType> requestedTypes
    ) {
        Set<InstitutionType> coveredTypes = new LinkedHashSet<>();
        if (provider.type() == InstitutionType.PHARMACY) {
            coveredTypes.add(InstitutionType.PHARMACY);
            return coveredTypes;
        }
        if (provider.type() == InstitutionType.EMERGENCY_ROOM) {
            coveredTypes.add(InstitutionType.EMERGENCY_ROOM);
            return coveredTypes;
        }
        if (requestedTypes.contains(InstitutionType.HOSPITAL)) {
            coveredTypes.add(InstitutionType.HOSPITAL);
        }
        return coveredTypes;
    }

    private List<NearbyInstitutionItemResponse> enrichPublicMetadata(
            List<NearbyInstitutionItemResponse> institutions,
            double searchLat,
            double searchLng,
            int radiusMeters,
            ZonedDateTime requestedAt,
            Set<InstitutionType> requestedTypes
    ) {
        List<OperatingHoursRequest> requests = operatingHoursRequests(
                institutions,
                searchLat,
                searchLng,
                requestedAt,
                requestedTypes
        );
        if (requests.isEmpty()) {
            return institutions;
        }

        List<PublicInstitutionDetails> publicInstitutions = new ArrayList<>();

        for (int start = 0; start < requests.size(); start += MAX_CONCURRENT_DETAIL_REQUESTS) {
            int end = Math.min(start + MAX_CONCURRENT_DETAIL_REQUESTS, requests.size());
            List<OperatingHoursRequest> requestBatch = requests.subList(start, end);
            List<CompletableFuture<PublicInstitutionLookup>> futures = requestBatch.stream()
                    .map(request -> CompletableFuture.supplyAsync(
                            () -> requestOperatingHours(request, requestedAt)
                    ))
                    .toList();

            for (int index = 0; index < futures.size(); index++) {
                try {
                    PublicInstitutionLookup lookup = futures.get(index).join();
                    publicInstitutions.addAll(lookup.institutions());
                } catch (CompletionException exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    OperatingHoursRequest request = requestBatch.get(index);
                    log.warn(
                            "{} {} {} 의료기관 기준정보 조회 실패: {}",
                            request.region().province(),
                            request.region().district(),
                            request.type(),
                            cause.getMessage()
                    );
                }
            }
        }

        List<PublicInstitutionDetails> consolidatedInstitutions = consolidatePublicInstitutions(
                publicInstitutions
        );
        if (consolidatedInstitutions.isEmpty()) {
            return institutions;
        }
        return mergePublicInstitutions(
                institutions,
                consolidatedInstitutions,
                searchLat,
                searchLng,
                radiusMeters,
                requestedAt,
                requestedTypes
        );
    }

    private List<OperatingHoursRequest> operatingHoursRequests(
            List<NearbyInstitutionItemResponse> institutions,
            double searchLat,
            double searchLng,
            ZonedDateTime requestedAt,
            Set<InstitutionType> requestedTypes
    ) {
        Set<OperatingHoursRequest> requests = new LinkedHashSet<>();
        Set<Region> nearbyRegions = institutions.stream()
                .map(NearbyInstitutionItemResponse::roadAddress)
                .map(Region::from)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (nearbyRegions.isEmpty()) {
            Region anchorRegion = requestNearbyRegion(searchLat, searchLng);
            if (anchorRegion != null) {
                nearbyRegions.add(anchorRegion);
            }
        }
        int dayIndex = requestedAt.getDayOfWeek().getValue();

        for (NearbyInstitutionItemResponse institution : institutions) {
            Region region = Region.from(institution.roadAddress());
            URI endpoint = publicDetailsEndpoint(institution.type());
            if (region != null) {
                requests.add(new OperatingHoursRequest(
                        endpoint,
                        institution.type(),
                        region,
                        dayIndex
                ));
                continue;
            }
            for (Region nearbyRegion : nearbyRegions) {
                requests.add(new OperatingHoursRequest(
                        endpoint,
                        institution.type(),
                        nearbyRegion,
                        dayIndex
                ));
            }
        }

        for (InstitutionType requestedType : requestedTypes) {
            URI endpoint = publicDetailsEndpoint(requestedType);
            for (Region nearbyRegion : nearbyRegions) {
                requests.add(new OperatingHoursRequest(
                        endpoint,
                        requestedType,
                        nearbyRegion,
                        dayIndex
                ));
            }
        }
        return List.copyOf(requests);
    }

    private URI publicDetailsEndpoint(InstitutionType type) {
        return switch (type) {
            case HOSPITAL -> properties.hospitalListUrl();
            case PHARMACY -> properties.pharmacyListUrl();
            case EMERGENCY_ROOM -> properties.emergencyListUrl();
        };
    }

    private PublicInstitutionLookup requestOperatingHours(
            OperatingHoursRequest request,
            ZonedDateTime requestedAt
    ) {
        OperatingHoursSearchKey cacheKey = new OperatingHoursSearchKey(
                request.type(),
                request.region().province(),
                request.region().district(),
                request.dayIndex()
        );
        CachedPublicInstitutionLookup cached = operatingHoursCache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.lookup();
        }

        try {
            int maxRows = Math.max(1, properties.maxOperatingHoursRows());
            List<PublicInstitutionDetails> institutions = new ArrayList<>();
            int pageNumber = 1;
            int fetchedRows = 0;

            while (fetchedRows < maxRows) {
                int pageSize = Math.min(DETAIL_PAGE_SIZE, maxRows - fetchedRows);
                URI requestUri = buildOperatingHoursRequestUri(request, pageNumber, pageSize);
                HttpRequest httpRequest = HttpRequest.newBuilder(requestUri)
                        .timeout(properties.timeout())
                        .header("Accept", "application/xml, text/xml")
                        .header("User-Agent", "medion-medical-search/1.0")
                        .GET()
                        .build();
                HttpResponse<byte[]> response = httpClient.send(
                        httpRequest,
                        HttpResponse.BodyHandlers.ofByteArray()
                );
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new PublicDataClientException(
                            "의료기관 기준정보 API가 HTTP " + response.statusCode() + "을 반환했습니다."
                    );
                }

                Document document = parseXml(response.body());
                verifySuccessfulResponse(document, "의료기관 기준정보");
                PublicInstitutionLookup page = parsePublicInstitutions(
                        document,
                        request.type(),
                        requestedAt
                );
                institutions.addAll(page.institutions());

                int responseItemCount = document.getElementsByTagName("item").getLength();
                fetchedRows += responseItemCount;
                Integer totalCount = parseNonNegativeInteger(documentText(document, "totalCount"));
                if (responseItemCount < pageSize
                        || (totalCount != null && pageNumber * pageSize >= totalCount)) {
                    break;
                }
                pageNumber++;
            }

            PublicInstitutionLookup result = new PublicInstitutionLookup(List.copyOf(institutions));
            if (!properties.operatingHoursCacheTtl().isNegative()
                    && !properties.operatingHoursCacheTtl().isZero()) {
                operatingHoursCache.put(
                        cacheKey,
                        new CachedPublicInstitutionLookup(
                                result,
                                Instant.now().plus(properties.operatingHoursCacheTtl())
                        )
                );
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublicDataClientException("의료기관 기준정보 API 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new PublicDataClientException("의료기관 기준정보 API에 연결할 수 없습니다.", exception);
        }
    }

    private URI buildOperatingHoursRequestUri(
            OperatingHoursRequest request,
            int pageNumber,
            int pageSize
    ) {
        String separator = request.endpoint().toString().contains("?") ? "&" : "?";
        StringBuilder query = new StringBuilder("serviceKey=")
                .append(encodedServiceKey())
                .append("&Q0=").append(encodeQueryValue(request.region().province()));
        if (!request.region().district().isBlank()) {
            query.append("&Q1=").append(encodeQueryValue(request.region().district()));
        }
        query.append("&ORD=NAME&pageNo=")
                .append(pageNumber)
                .append("&numOfRows=")
                .append(pageSize);
        return URI.create(request.endpoint() + separator + query);
    }

    private String encodeQueryValue(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private PublicInstitutionLookup parsePublicInstitutions(
            Document document,
            InstitutionType type,
            ZonedDateTime requestedAt
    ) {
        NodeList itemNodes = document.getElementsByTagName("item");
        List<PublicInstitutionDetails> institutions = new ArrayList<>();

        for (int index = 0; index < itemNodes.getLength(); index++) {
            Node itemNode = itemNodes.item(index);
            String name = childText(itemNode, "dutyName", "name");
            String address = childText(itemNode, "dutyAddr", "address");
            if (name == null) {
                continue;
            }
            String externalKey = firstNonBlank(
                    childText(itemNode, "hpid", "ykiho"),
                    name + "|" + address
            );
            OperatingHours operatingHours = resolveOperatingHours(itemNode, requestedAt);
            String institutionKind = resolvePublicDataInstitutionKind(itemNode, type);
            List<String> medicalDepartments = List.copyOf(
                    resolvePublicMedicalDepartments(itemNode, type, institutionKind)
            );
            institutions.add(new PublicInstitutionDetails(
                    externalId(type, externalKey),
                    type,
                    name,
                    institutionKind,
                    medicalDepartments,
                    resolvePublicPhoneNumber(itemNode, type),
                    address,
                    parseDecimal(childText(itemNode, "wgs84Lat", "latitude")),
                    parseDecimal(childText(itemNode, "wgs84Lon", "longitude")),
                    operatingHours
            ));
        }
        return new PublicInstitutionLookup(List.copyOf(institutions));
    }

    private List<PublicInstitutionDetails> consolidatePublicInstitutions(
            List<PublicInstitutionDetails> institutions
    ) {
        Map<Long, PublicInstitutionDetails> institutionsById = new LinkedHashMap<>();
        for (PublicInstitutionDetails institution : institutions) {
            institutionsById.merge(institution.id(), institution, this::mergePublicInstitutionDetails);
        }
        return List.copyOf(institutionsById.values());
    }

    private PublicInstitutionDetails mergePublicInstitutionDetails(
            PublicInstitutionDetails first,
            PublicInstitutionDetails second
    ) {
        Set<String> medicalDepartments = new LinkedHashSet<>(first.medicalDepartments());
        medicalDepartments.addAll(second.medicalDepartments());
        OperatingHours operatingHours = mergeOperatingHours(
                first.operatingHours(),
                second.operatingHours()
        );
        return new PublicInstitutionDetails(
                first.id(),
                first.type(),
                firstNonBlank(first.name(), second.name()),
                firstNonBlank(first.institutionKind(), second.institutionKind()),
                List.copyOf(medicalDepartments),
                firstNonBlank(first.phoneNumber(), second.phoneNumber()),
                firstNonBlank(first.roadAddress(), second.roadAddress()),
                firstNonNull(first.latitude(), second.latitude()),
                firstNonNull(first.longitude(), second.longitude()),
                operatingHours
        );
    }

    private List<NearbyInstitutionItemResponse> mergePublicInstitutions(
            List<NearbyInstitutionItemResponse> existingInstitutions,
            List<PublicInstitutionDetails> publicInstitutions,
            double searchLat,
            double searchLng,
            int radiusMeters,
            ZonedDateTime requestedAt,
            Set<InstitutionType> requestedTypes
    ) {
        Map<Long, NearbyInstitutionItemResponse> mergedById = new LinkedHashMap<>();
        Set<Long> matchedPublicInstitutionIds = new LinkedHashSet<>();

        for (NearbyInstitutionItemResponse item : existingInstitutions) {
            PublicInstitutionDetails match = findPublicInstitution(item, publicInstitutions);
            if (match != null) {
                NearbyInstitutionItemResponse merged = withPublicMetadata(item, match);
                mergedById.putIfAbsent(merged.id(), merged);
                matchedPublicInstitutionIds.add(match.id());
                continue;
            }
            mergedById.putIfAbsent(item.id(), item);
        }

        for (PublicInstitutionDetails publicInstitution : publicInstitutions) {
            if (matchedPublicInstitutionIds.contains(publicInstitution.id())
                    || !requestedTypes.contains(publicInstitution.type())) {
                continue;
            }
            NearbyInstitutionItemResponse nearbyInstitution = toNearbyInstitution(
                    publicInstitution,
                    searchLat,
                    searchLng,
                    radiusMeters,
                    requestedAt
            );
            if (nearbyInstitution != null) {
                mergedById.putIfAbsent(nearbyInstitution.id(), nearbyInstitution);
            }
        }

        return mergedById.values().stream()
                .sorted(Comparator.comparingLong(NearbyInstitutionItemResponse::distanceMeters))
                .toList();
    }

    private NearbyInstitutionItemResponse toNearbyInstitution(
            PublicInstitutionDetails publicInstitution,
            double searchLat,
            double searchLng,
            int radiusMeters,
            ZonedDateTime requestedAt
    ) {
        if (publicInstitution.latitude() == null || publicInstitution.longitude() == null) {
            return null;
        }
        long distanceMeters = Math.round(distanceMeters(
                searchLat,
                searchLng,
                publicInstitution.latitude().doubleValue(),
                publicInstitution.longitude().doubleValue()
        ));
        if (distanceMeters > radiusMeters) {
            return null;
        }

        OperatingHours operatingHours = publicInstitution.operatingHours();
        return new NearbyInstitutionItemResponse(
                publicInstitution.id(),
                publicInstitution.type(),
                publicInstitution.name(),
                publicInstitution.institutionKind(),
                Set.copyOf(publicInstitution.medicalDepartments()),
                publicInstitution.phoneNumber(),
                publicInstitution.roadAddress(),
                publicInstitution.latitude(),
                publicInstitution.longitude(),
                distanceMeters,
                operatingHours.open(),
                operatingHours.known(),
                operatingHours.openTime(),
                operatingHours.closeTime(),
                null,
                operatingHours.schedules(),
                requestedAt.toLocalDateTime()
        );
    }

    private PublicInstitutionDetails findPublicInstitution(
            NearbyInstitutionItemResponse item,
            List<PublicInstitutionDetails> candidates
    ) {
        for (PublicInstitutionDetails candidate : candidates) {
            if (candidate.type() == item.type() && candidate.id() == item.id()) {
                return candidate;
            }
        }

        String itemName = normalizeInstitutionName(item.name());
        String itemPhone = normalizePhoneNumber(item.phoneNumber());
        String itemAddress = normalizeAddress(item.roadAddress());
        PublicInstitutionDetails bestMatch = null;
        int bestScore = 0;

        for (PublicInstitutionDetails candidate : candidates) {
            if (candidate.type() != item.type()) {
                continue;
            }
            int score = publicInstitutionMatchScore(
                    item,
                    itemName,
                    itemPhone,
                    itemAddress,
                    candidate
            );
            if (score > bestScore) {
                bestScore = score;
                bestMatch = candidate;
            }
        }
        return bestScore >= 60 ? bestMatch : null;
    }

    private int publicInstitutionMatchScore(
            NearbyInstitutionItemResponse item,
            String itemName,
            String itemPhone,
            String itemAddress,
            PublicInstitutionDetails candidate
    ) {
        int score = 0;
        String candidateName = normalizeInstitutionName(candidate.name());
        String candidatePhone = normalizePhoneNumber(candidate.phoneNumber());
        String candidateAddress = normalizeAddress(candidate.roadAddress());

        if (itemPhone != null && itemPhone.equals(candidatePhone)) {
            score += 100;
        }
        if (itemAddress != null && candidateAddress != null) {
            if (itemAddress.equals(candidateAddress)) {
                score += 80;
            } else if (itemAddress.length() >= 8 && candidateAddress.length() >= 8
                    && (itemAddress.contains(candidateAddress) || candidateAddress.contains(itemAddress))) {
                score += 45;
            }
        }
        if (itemName != null && candidateName != null) {
            if (itemName.equals(candidateName)) {
                score += 60;
            } else if (itemName.length() >= 4 && candidateName.length() >= 4
                    && (itemName.endsWith(candidateName) || candidateName.endsWith(itemName))) {
                score += 35;
            }
        }
        if (candidate.latitude() != null && candidate.longitude() != null) {
            double distance = distanceMeters(
                    item.latitude().doubleValue(),
                    item.longitude().doubleValue(),
                    candidate.latitude().doubleValue(),
                    candidate.longitude().doubleValue()
            );
            if (distance <= 100) {
                score += 40;
            } else if (distance <= 500) {
                score += 25;
            } else if (distance <= 2_000) {
                score += 10;
            }
        }
        return score;
    }

    private NearbyInstitutionItemResponse withPublicMetadata(
            NearbyInstitutionItemResponse item,
            PublicInstitutionDetails publicInstitution
    ) {
        if (publicInstitution == null) {
            return item;
        }
        OperatingHours publicHours = publicInstitution.operatingHours();
        boolean usePublicHours = publicHours != null && publicHours.known();
        return new NearbyInstitutionItemResponse(
                item.id(),
                item.type(),
                firstNonBlank(publicInstitution.name(), item.name()),
                firstNonBlank(publicInstitution.institutionKind(), item.institutionKind()),
                mergeMedicalDepartments(
                        item.medicalDepartments(),
                        publicInstitution.medicalDepartments()
                ),
                firstNonBlank(publicInstitution.phoneNumber(), item.phoneNumber()),
                firstNonBlank(publicInstitution.roadAddress(), item.roadAddress()),
                item.latitude(),
                item.longitude(),
                item.distanceMeters(),
                usePublicHours ? publicHours.open() : item.open(),
                usePublicHours || item.operatingHoursKnown(),
                usePublicHours ? publicHours.openTime() : item.todayOpenTime(),
                usePublicHours ? publicHours.closeTime() : item.todayCloseTime(),
                item.availableEmergencyBeds(),
                mergeOperatingSchedules(item.operatingSchedules(), publicHours.schedules()),
                item.lastSyncedAt()
        );
    }

    private List<NearbyInstitutionItemResponse> enrichEmergencyAvailability(
            List<NearbyInstitutionItemResponse> institutions,
            ZonedDateTime requestedAt
    ) {
        if (!properties.emergencyMedicalEnabled()) {
            return institutions;
        }

        Set<EmergencyAvailabilityRequest> requests = new LinkedHashSet<>();
        for (NearbyInstitutionItemResponse institution : institutions) {
            if (institution.type() != InstitutionType.EMERGENCY_ROOM) {
                continue;
            }
            Region region = Region.from(institution.roadAddress());
            if (region != null && !region.district().isBlank()) {
                requests.add(new EmergencyAvailabilityRequest(region));
            }
        }
        if (requests.isEmpty()) {
            return institutions;
        }

        List<EmergencyAvailabilityRequest> requestList = List.copyOf(requests);
        Map<Long, EmergencyAvailability> byInstitutionId = new LinkedHashMap<>();
        Map<String, EmergencyAvailability> byInstitutionName = new LinkedHashMap<>();

        for (int start = 0; start < requestList.size(); start += MAX_CONCURRENT_DETAIL_REQUESTS) {
            int end = Math.min(start + MAX_CONCURRENT_DETAIL_REQUESTS, requestList.size());
            List<EmergencyAvailabilityRequest> requestBatch = requestList.subList(start, end);
            List<CompletableFuture<EmergencyAvailabilityLookup>> futures = requestBatch.stream()
                    .map(request -> CompletableFuture.supplyAsync(
                            () -> requestEmergencyAvailability(request)
                    ))
                    .toList();

            for (int index = 0; index < futures.size(); index++) {
                try {
                    EmergencyAvailabilityLookup lookup = futures.get(index).join();
                    byInstitutionId.putAll(lookup.byInstitutionId());
                    byInstitutionName.putAll(lookup.byInstitutionName());
                } catch (CompletionException exception) {
                    Throwable cause = exception.getCause() == null ? exception : exception.getCause();
                    Region region = requestBatch.get(index).region();
                    log.warn(
                            "{} {} 실시간 응급실 병상 조회 실패: {}",
                            region.province(),
                            region.district(),
                            cause.getMessage()
                    );
                }
            }
        }

        return institutions.stream()
                .map(item -> {
                    if (item.type() != InstitutionType.EMERGENCY_ROOM) {
                        return item;
                    }
                    EmergencyAvailability availability = firstNonNull(
                            byInstitutionId.get(item.id()),
                            findByInstitutionName(item.name(), byInstitutionName)
                    );
                    return withEmergencyAvailability(item, availability, requestedAt);
                })
                .toList();
    }

    private EmergencyAvailabilityLookup requestEmergencyAvailability(EmergencyAvailabilityRequest request) {
        EmergencyAvailabilitySearchKey cacheKey = new EmergencyAvailabilitySearchKey(
                request.region().province(),
                request.region().district()
        );
        CachedEmergencyAvailability cached = emergencyAvailabilityCache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.lookup();
        }

        URI requestUri = buildEmergencyAvailabilityRequestUri(request.region());
        HttpRequest httpRequest = HttpRequest.newBuilder(requestUri)
                .timeout(properties.timeout())
                .header("Accept", "application/xml, text/xml")
                .header("User-Agent", "medion-medical-search/1.0")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PublicDataClientException(
                        "실시간 응급실 병상 API가 HTTP " + response.statusCode() + "을 반환했습니다."
                );
            }

            Document document = parseXml(response.body());
            verifySuccessfulResponse(document, "실시간 응급실 병상");
            EmergencyAvailabilityLookup result = parseEmergencyAvailability(document);
            if (!properties.emergencyAvailabilityCacheTtl().isNegative()
                    && !properties.emergencyAvailabilityCacheTtl().isZero()) {
                emergencyAvailabilityCache.put(
                        cacheKey,
                        new CachedEmergencyAvailability(
                                result,
                                Instant.now().plus(properties.emergencyAvailabilityCacheTtl())
                        )
                );
            }
            return result;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublicDataClientException("실시간 응급실 병상 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new PublicDataClientException("실시간 응급실 병상 API에 연결할 수 없습니다.", exception);
        }
    }

    private URI buildEmergencyAvailabilityRequestUri(Region region) {
        URI endpoint = properties.emergencyAvailabilityUrl();
        String separator = endpoint.toString().contains("?") ? "&" : "?";
        String query = "serviceKey=" + encodedServiceKey()
                + "&STAGE1=" + encodeQueryValue(region.province())
                + "&STAGE2=" + encodeQueryValue(region.district())
                + "&pageNo=1"
                + "&numOfRows=" + Math.max(
                        1,
                        Math.min(properties.maxResults(), EMERGENCY_AVAILABILITY_PAGE_SIZE)
                );
        return URI.create(endpoint + separator + query);
    }

    private EmergencyAvailabilityLookup parseEmergencyAvailability(Document document) {
        NodeList itemNodes = document.getElementsByTagName("item");
        Map<Long, EmergencyAvailability> byInstitutionId = new LinkedHashMap<>();
        Map<String, EmergencyAvailability> byInstitutionName = new LinkedHashMap<>();

        for (int index = 0; index < itemNodes.getLength(); index++) {
            Node itemNode = itemNodes.item(index);
            Integer availableBeds = parseNonNegativeInteger(childText(itemNode, "hvec"));
            String emergencyPhone = childText(itemNode, "dutytel3", "dutyTel3", "hv1");
            EmergencyAvailability availability = new EmergencyAvailability(availableBeds, emergencyPhone);

            String externalKey = childText(itemNode, "hpid");
            if (externalKey != null) {
                byInstitutionId.put(
                        externalId(InstitutionType.EMERGENCY_ROOM, externalKey),
                        availability
                );
            }
            String name = normalizeInstitutionName(childText(itemNode, "dutyname", "dutyName"));
            if (name != null) {
                byInstitutionName.putIfAbsent(name, availability);
            }
        }
        return new EmergencyAvailabilityLookup(
                Map.copyOf(byInstitutionId),
                Map.copyOf(byInstitutionName)
        );
    }

    private NearbyInstitutionItemResponse withEmergencyAvailability(
            NearbyInstitutionItemResponse item,
            EmergencyAvailability availability,
            ZonedDateTime requestedAt
    ) {
        if (availability == null) {
            return item;
        }
        return new NearbyInstitutionItemResponse(
                item.id(),
                item.type(),
                item.name(),
                item.institutionKind(),
                item.medicalDepartments(),
                firstNonBlank(availability.emergencyPhone(), item.phoneNumber()),
                item.roadAddress(),
                item.latitude(),
                item.longitude(),
                item.distanceMeters(),
                item.open(),
                item.operatingHoursKnown(),
                item.todayOpenTime(),
                item.todayCloseTime(),
                availability.availableBeds(),
                item.operatingSchedules(),
                requestedAt.toLocalDateTime()
        );
    }

    private Region requestNearbyRegion(double searchLat, double searchLng) {
        List<RegionAnchorRequest> anchorRequests = List.of(
                new RegionAnchorRequest(properties.pharmacyUrl(), "약국"),
                new RegionAnchorRequest(properties.emergencyUrl(), "응급의료기관")
        );
        for (RegionAnchorRequest anchorRequest : anchorRequests) {
            try {
                Region region = requestNearbyRegion(anchorRequest, searchLat, searchLng);
                if (region != null) {
                    return region;
                }
            } catch (PublicDataClientException exception) {
                log.warn("{} 위치정보로 지역 확인 실패: {}", anchorRequest.label(), exception.getMessage());
            }
        }
        return null;
    }

    private Region requestNearbyRegion(
            RegionAnchorRequest anchorRequest,
            double searchLat,
            double searchLng
    ) {
        URI requestUri = buildRequestUri(anchorRequest.endpoint(), searchLat, searchLng, 10);
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(properties.timeout())
                .header("Accept", "application/xml, text/xml")
                .header("User-Agent", "medion-medical-search/1.0")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PublicDataClientException(
                        anchorRequest.label() + " 위치 API가 HTTP " + response.statusCode() + "을 반환했습니다."
                );
            }
            Document document = parseXml(response.body());
            verifySuccessfulResponse(document, anchorRequest.label() + " 위치");
            NodeList itemNodes = document.getElementsByTagName("item");
            for (int index = 0; index < itemNodes.getLength(); index++) {
                Region region = Region.from(childText(itemNodes.item(index), "dutyAddr", "address"));
                if (region != null) {
                    return region;
                }
            }
            return null;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublicDataClientException(anchorRequest.label() + " 위치 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new PublicDataClientException(anchorRequest.label() + " 위치 API에 연결할 수 없습니다.", exception);
        }
    }

    private List<NearbyInstitutionItemResponse> requestProvider(
            ProviderRequest provider,
            double searchLat,
            double searchLng,
            int radiusMeters,
            Set<InstitutionType> allowedTypes,
            ZonedDateTime requestedAt
    ) {
        URI requestUri = buildRequestUri(provider.endpoint(), searchLat, searchLng);
        HttpRequest request = HttpRequest.newBuilder(requestUri)
                .timeout(properties.timeout())
                .header("Accept", "application/xml, text/xml")
                .header("User-Agent", "medion-medical-search/1.0")
                .GET()
                .build();

        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PublicDataClientException(
                        provider.label() + " API가 HTTP " + response.statusCode() + "을 반환했습니다."
                );
            }

            Document document = parseXml(response.body());
            verifySuccessfulResponse(document, provider.label());
            return mapItems(
                    document,
                    provider.type(),
                    searchLat,
                    searchLng,
                    radiusMeters,
                    allowedTypes,
                    requestedAt
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PublicDataClientException(provider.label() + " API 조회가 중단되었습니다.", exception);
        } catch (IOException exception) {
            throw new PublicDataClientException(provider.label() + " API에 연결할 수 없습니다.", exception);
        }
    }

    private URI buildRequestUri(URI endpoint, double lat, double lng) {
        return buildRequestUri(
                endpoint,
                lat,
                lng,
                Math.max(1, Math.min(properties.maxResults(), 1000))
        );
    }

    private URI buildRequestUri(URI endpoint, double lat, double lng, int numOfRows) {
        String separator = endpoint.toString().contains("?") ? "&" : "?";
        String query = "serviceKey=" + encodedServiceKey()
                + "&WGS84_LON=" + String.format(Locale.ROOT, "%.7f", lng)
                + "&WGS84_LAT=" + String.format(Locale.ROOT, "%.7f", lat)
                + "&pageNo=1"
                + "&numOfRows=" + Math.max(1, Math.min(numOfRows, 1000));
        return URI.create(endpoint + separator + query);
    }

    private String encodedServiceKey() {
        String serviceKey = properties.serviceKey().trim();
        if (ENCODED_CHARACTER_PATTERN.matcher(serviceKey).find()) {
            serviceKey = URLDecoder.decode(serviceKey, StandardCharsets.UTF_8);
        }
        return URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);
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

    private void verifySuccessfulResponse(Document document, String providerLabel) {
        String resultCode = documentText(document, "resultCode");
        if (resultCode != null && !"00".equals(resultCode) && !"0000".equals(resultCode)) {
            String resultMessage = firstNonBlank(
                    documentText(document, "resultMsg"),
                    documentText(document, "resultMag"),
                    "알 수 없는 오류"
            );
            throw new PublicDataClientException(
                    providerLabel + " API 오류(" + resultCode + "): " + resultMessage
            );
        }

        String gatewayErrorCode = documentText(document, "returnReasonCode");
        if (gatewayErrorCode != null && !"00".equals(gatewayErrorCode) && !"0".equals(gatewayErrorCode)) {
            String gatewayMessage = firstNonBlank(
                    documentText(document, "returnAuthMsg"),
                    documentText(document, "errMsg"),
                    "인증 오류"
            );
            throw new PublicDataClientException(
                    providerLabel + " API 인증 오류(" + gatewayErrorCode + "): " + gatewayMessage
            );
        }
    }

    private List<NearbyInstitutionItemResponse> mapItems(
            Document document,
            InstitutionType providerType,
            double searchLat,
            double searchLng,
            int radiusMeters,
            Set<InstitutionType> allowedTypes,
            ZonedDateTime requestedAt
    ) {
        NodeList itemNodes = document.getElementsByTagName("item");
        List<NearbyInstitutionItemResponse> items = new ArrayList<>();

        for (int index = 0; index < itemNodes.getLength(); index++) {
            Node itemNode = itemNodes.item(index);
            String name = childText(itemNode, "dutyName", "name");
            BigDecimal latitude = parseDecimal(childText(itemNode, "wgs84Lat", "latitude"));
            BigDecimal longitude = parseDecimal(childText(itemNode, "wgs84Lon", "longitude"));
            if (name == null || latitude == null || longitude == null) {
                continue;
            }

            double itemLat = latitude.doubleValue();
            double itemLng = longitude.doubleValue();
            long distanceMeters = Math.round(distanceMeters(searchLat, searchLng, itemLat, itemLng));
            if (distanceMeters > radiusMeters) {
                continue;
            }

            InstitutionType type = resolveType(itemNode, providerType, allowedTypes);
            if (type == null || !allowedTypes.contains(type)) {
                continue;
            }

            OperatingHours operatingHours = resolveOperatingHours(itemNode, requestedAt);
            String institutionKind = resolvePublicDataInstitutionKind(itemNode, type);
            String externalKey = firstNonBlank(
                    childText(itemNode, "hpid", "ykiho"),
                    name + "|" + childText(itemNode, "dutyAddr", "address")
            );
            items.add(new NearbyInstitutionItemResponse(
                    externalId(type, externalKey),
                    type,
                    name,
                    institutionKind,
                    resolvePublicMedicalDepartments(itemNode, type, institutionKind),
                    resolvePublicPhoneNumber(itemNode, type),
                    childText(itemNode, "dutyAddr", "address"),
                    latitude,
                    longitude,
                    distanceMeters,
                    operatingHours.open(),
                    operatingHours.known(),
                    operatingHours.openTime(),
                    operatingHours.closeTime(),
                    null,
                    operatingHours.schedules(),
                    requestedAt.toLocalDateTime()
            ));
        }

        return items;
    }

    private String resolvePublicDataInstitutionKind(
            Node itemNode,
            InstitutionType type
    ) {
        if (type == InstitutionType.PHARMACY) {
            return null;
        }
        if (type == InstitutionType.EMERGENCY_ROOM) {
            return firstNonBlank(
                    childText(itemNode, "dutyEmclsName", "dutyEmclsNam"),
                    childText(itemNode, "dutyDivName", "dutyDivNam"),
                    "응급의료기관"
            );
        }
        String officialKind = childText(itemNode, "dutyDivName", "dutyDivNam");
        if (officialKind != null) {
            return officialKind;
        }
        return type == InstitutionType.EMERGENCY_ROOM ? "응급의료기관" : "병원";
    }

    private Set<String> resolvePublicMedicalDepartments(
            Node itemNode,
            InstitutionType type,
            String institutionKind
    ) {
        if (type != InstitutionType.HOSPITAL) {
            return Set.of();
        }
        Set<String> departments = new LinkedHashSet<>(officialMedicalDepartments(itemNode));
        if (institutionKind != null) {
            switch (institutionKind.trim()) {
                case "치과의원", "치과병원" ->
                        departments.add(HospitalDepartment.DENTISTRY.getDisplayName());
                case "한의원", "한방병원" ->
                        departments.add(HospitalDepartment.KOREAN_CLINIC.getDisplayName());
                default -> {
                }
            }
        }
        return Set.copyOf(departments);
    }

    private String resolvePublicPhoneNumber(Node itemNode, InstitutionType type) {
        return type == InstitutionType.EMERGENCY_ROOM
                ? childText(itemNode, "dutyTel3", "dutyTel1", "phone")
                : childText(itemNode, "dutyTel1", "dutyTel3", "phone");
    }

    private List<String> officialMedicalDepartments(Node itemNode) {
        String value = childText(
                itemNode,
                "dgidIdName",
                "dgidIdNam",
                "medicalDepartmentName",
                "medicalDepartmentNames"
        );
        if (value == null) {
            return List.of();
        }

        Set<String> departments = new LinkedHashSet<>();
        for (String part : MEDICAL_DEPARTMENT_SEPARATOR.split(value)) {
            String department = normalizeMedicalDepartmentName(part);
            if (department != null) {
                departments.add(department);
            }
        }
        return List.copyOf(departments);
    }

    private String normalizeMedicalDepartmentName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        String normalized = trimmed.replaceAll("\\s+", "");
        for (HospitalDepartment department : HospitalDepartment.values()) {
            if (department.identifies(normalized)) {
                return department.getDisplayName();
            }
        }
        return switch (normalized) {
            case "소아과" -> HospitalDepartment.PEDIATRICS.getDisplayName();
            default -> null;
        };
    }

    private InstitutionType resolveType(
            Node itemNode,
            InstitutionType providerType,
            Set<InstitutionType> allowedTypes
    ) {
        if (providerType == InstitutionType.PHARMACY) {
            return InstitutionType.PHARMACY;
        }
        if (providerType == InstitutionType.EMERGENCY_ROOM) {
            return InstitutionType.EMERGENCY_ROOM;
        }
        if (!allowedTypes.contains(InstitutionType.HOSPITAL)
                && allowedTypes.contains(InstitutionType.EMERGENCY_ROOM)) {
            String emergencyRoomStatus = childText(itemNode, "dutyEryn", "emergencyRoom");
            return "1".equals(emergencyRoomStatus) || "Y".equalsIgnoreCase(emergencyRoomStatus)
                    ? InstitutionType.EMERGENCY_ROOM
                    : null;
        }
        return InstitutionType.HOSPITAL;
    }

    private OperatingHours resolveOperatingHours(Node itemNode, ZonedDateTime requestedAt) {
        Set<OperatingScheduleFilter> schedules = resolveOperatingSchedules(itemNode);
        int dayIndex = requestedAt.getDayOfWeek().getValue();
        String openValue = childText(itemNode, "startTime", "dutyTime" + dayIndex + "s");
        String closeValue = childText(
                itemNode,
                "endTime",
                "dutyTime" + dayIndex + "c",
                "dutyTime" + dayIndex + "e"
        );
        LocalTime openTime = parseTime(openValue);
        LocalTime closeTime = parseTime(closeValue);
        if (openValue == null && closeValue == null && hasWeeklyOperatingHours(itemNode)) {
            return OperatingHours.closed(schedules);
        }
        if (openTime == null || closeTime == null) {
            return OperatingHours.unknown(schedules);
        }

        LocalTime currentTime = requestedAt.toLocalTime();
        boolean open;
        if (openTime.isBefore(closeTime)) {
            open = !currentTime.isBefore(openTime) && currentTime.isBefore(closeTime);
        } else if (openTime.isAfter(closeTime)) {
            open = !currentTime.isBefore(openTime) || currentTime.isBefore(closeTime);
        } else {
            open = LocalTime.MIDNIGHT.equals(openTime);
        }
        return new OperatingHours(true, open, openTime, closeTime, schedules);
    }

    private Set<OperatingScheduleFilter> resolveOperatingSchedules(Node itemNode) {
        Set<OperatingScheduleFilter> schedules = EnumSet.noneOf(OperatingScheduleFilter.class);
        if (hasOperatingHours(itemNode, 6)) {
            schedules.add(OperatingScheduleFilter.SATURDAY);
        }
        if (hasOperatingHours(itemNode, 7)) {
            schedules.add(OperatingScheduleFilter.SUNDAY);
        }
        if (hasOperatingHours(itemNode, 8)) {
            schedules.add(OperatingScheduleFilter.HOLIDAY);
        }

        for (int dayIndex = 1; dayIndex <= 8; dayIndex++) {
            Integer openMinutes = operatingTimeMinutes(itemNode, dayIndex, true);
            Integer closeMinutes = operatingTimeMinutes(itemNode, dayIndex, false);
            if (openMinutes == null || closeMinutes == null) {
                continue;
            }
            if (openMinutes == 0 && (closeMinutes == 0 || closeMinutes == 24 * 60)) {
                schedules.add(OperatingScheduleFilter.TWENTY_FOUR_HOURS);
                schedules.add(OperatingScheduleFilter.NIGHT);
            } else if (closeMinutes >= 20 * 60 || closeMinutes <= openMinutes) {
                schedules.add(OperatingScheduleFilter.NIGHT);
            }
        }
        return Set.copyOf(schedules);
    }

    private boolean hasOperatingHours(Node itemNode, int dayIndex) {
        return operatingTimeMinutes(itemNode, dayIndex, true) != null
                && operatingTimeMinutes(itemNode, dayIndex, false) != null;
    }

    private Integer operatingTimeMinutes(Node itemNode, int dayIndex, boolean openingTime) {
        String value = openingTime
                ? childText(itemNode, "dutyTime" + dayIndex + "s")
                : childText(
                        itemNode,
                        "dutyTime" + dayIndex + "c",
                        "dutyTime" + dayIndex + "e"
                );
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
            return null;
        }
        int hour = Integer.parseInt(digits.substring(0, 2));
        int minute = Integer.parseInt(digits.substring(2, 4));
        if (hour > 24 || minute > 59 || (hour == 24 && minute != 0)) {
            return null;
        }
        return hour * 60 + minute;
    }

    private boolean hasWeeklyOperatingHours(Node itemNode) {
        for (int dayIndex = 1; dayIndex <= 8; dayIndex++) {
            if (childText(
                    itemNode,
                    "dutyTime" + dayIndex + "s",
                    "dutyTime" + dayIndex + "c",
                    "dutyTime" + dayIndex + "e"
            ) != null) {
                return true;
            }
        }
        return false;
    }

    private LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() < 3 || digits.length() > 6) {
            return null;
        }
        if (digits.length() == 3) {
            digits = "0" + digits;
        } else if (digits.length() > 4) {
            digits = digits.substring(0, 4);
        }

        int hour = Integer.parseInt(digits.substring(0, 2));
        int minute = Integer.parseInt(digits.substring(2, 4));
        if (hour == 24 && minute == 0) {
            return LocalTime.MAX;
        }
        if (hour > 23 || minute > 59) {
            return null;
        }
        return LocalTime.of(hour, minute);
    }

    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer parseNonNegativeInteger(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed < 0 ? null : parsed;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String documentText(Document document, String tagName) {
        NodeList nodes = document.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return null;
        }
        return trimmedText(nodes.item(0));
    }

    private String childText(Node parent, String... names) {
        NodeList children = parent.getChildNodes();
        for (String name : names) {
            for (int index = 0; index < children.getLength(); index++) {
                Node child = children.item(index);
                if (child.getNodeType() == Node.ELEMENT_NODE
                        && child.getNodeName().equalsIgnoreCase(name)) {
                    String value = trimmedText(child);
                    if (value != null) {
                        return value;
                    }
                }
            }
        }
        return null;
    }

    private String trimmedText(Node node) {
        String value = node == null ? null : node.getTextContent();
        return value == null || value.isBlank() ? null : value.trim();
    }

    private long externalId(InstitutionType type, String externalKey) {
        CRC32 checksum = new CRC32();
        checksum.update((type.name() + ":" + externalKey).getBytes(StandardCharsets.UTF_8));
        long prefix = switch (type) {
            case HOSPITAL -> 1_000_000_000_000L;
            case PHARMACY -> 2_000_000_000_000L;
            case EMERGENCY_ROOM -> 3_000_000_000_000L;
        };
        return prefix + checksum.getValue();
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

    private <T> T firstNonNull(T first, T second) {
        return first == null ? second : first;
    }

    private String normalizeInstitutionName(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replaceAll("\\([^)]*\\)", "")
                .replace("의료법인", "")
                .replace("재단법인", "")
                .replace("사단법인", "")
                .replace("학교법인", "")
                .replace("사회복지법인", "")
                .replaceAll("[^\\p{L}\\p{N}]", "");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizePhoneNumber(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9]", "");
        if (normalized.startsWith("82") && normalized.length() >= 10) {
            normalized = "0" + normalized.substring(2);
        }
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeAddress(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT)
                .replace("서울특별시", "서울")
                .replace("부산광역시", "부산")
                .replace("대구광역시", "대구")
                .replace("인천광역시", "인천")
                .replace("광주광역시", "광주")
                .replace("대전광역시", "대전")
                .replace("울산광역시", "울산")
                .replace("세종특별자치시", "세종")
                .replaceAll("[^\\p{L}\\p{N}]", "");
        return normalized.isBlank() ? null : normalized;
    }

    private <T> T findByInstitutionName(String institutionName, Map<String, T> valuesByName) {
        String normalizedName = normalizeInstitutionName(institutionName);
        if (normalizedName == null) {
            return null;
        }
        T exactMatch = valuesByName.get(normalizedName);
        if (exactMatch != null || normalizedName.length() < 4) {
            return exactMatch;
        }

        String bestMatch = null;
        for (String candidate : valuesByName.keySet()) {
            if (candidate.length() < 4) {
                continue;
            }
            if (candidate.endsWith(normalizedName) || normalizedName.endsWith(candidate)) {
                if (bestMatch == null || candidate.length() > bestMatch.length()) {
                    bestMatch = candidate;
                }
            }
        }
        return bestMatch == null ? null : valuesByName.get(bestMatch);
    }

    private void removeExpiredCacheEntries() {
        if (cache.size() < 100) {
            return;
        }
        Instant now = Instant.now();
        cache.entrySet().removeIf(entry -> entry.getValue().expiresAt()
                .plus(STALE_CACHE_RETENTION)
                .isBefore(now));
    }

    private record ProviderRequest(URI endpoint, InstitutionType type, String label) {
    }

    private record RegionAnchorRequest(URI endpoint, String label) {
    }

    private record OperatingHoursRequest(
            URI endpoint,
            InstitutionType type,
            Region region,
            int dayIndex
    ) {
    }

    private record EmergencyAvailabilityRequest(Region region) {
    }

    private record Region(String province, String district) {
        private static Region from(String address) {
            if (address == null || address.isBlank()) {
                return null;
            }
            String[] parts = address.trim().split("\\s+");
            if (parts.length < 2) {
                return null;
            }

            String district = isDistrict(parts[1]) ? parts[1] : "";
            if (parts[1].endsWith("시") && parts.length > 2 && parts[2].endsWith("구")) {
                district = parts[1] + " " + parts[2];
            }
            return new Region(normalizeProvince(parts[0]), district);
        }

        private static String normalizeProvince(String value) {
            return switch (value) {
                case "서울" -> "서울특별시";
                case "부산" -> "부산광역시";
                case "대구" -> "대구광역시";
                case "인천" -> "인천광역시";
                case "광주" -> "광주광역시";
                case "대전" -> "대전광역시";
                case "울산" -> "울산광역시";
                case "세종" -> "세종특별자치시";
                case "경기" -> "경기도";
                case "강원", "강원도" -> "강원특별자치도";
                case "충북" -> "충청북도";
                case "충남" -> "충청남도";
                case "전북", "전라북도" -> "전북특별자치도";
                case "전남" -> "전라남도";
                case "경북" -> "경상북도";
                case "경남" -> "경상남도";
                case "제주", "제주도" -> "제주특별자치도";
                default -> value;
            };
        }

        private static boolean isDistrict(String value) {
            return value.endsWith("시") || value.endsWith("군") || value.endsWith("구");
        }
    }

    private OperatingHours mergeOperatingHours(OperatingHours first, OperatingHours second) {
        OperatingHours preferred = first.known() ? first : second;
        return new OperatingHours(
                preferred.known(),
                preferred.open(),
                preferred.openTime(),
                preferred.closeTime(),
                mergeOperatingSchedules(first.schedules(), second.schedules())
        );
    }

    private Set<OperatingScheduleFilter> mergeOperatingSchedules(
            Set<OperatingScheduleFilter> first,
            Set<OperatingScheduleFilter> second
    ) {
        Set<OperatingScheduleFilter> schedules = EnumSet.noneOf(OperatingScheduleFilter.class);
        schedules.addAll(first);
        schedules.addAll(second);
        return Set.copyOf(schedules);
    }

    private Set<String> mergeMedicalDepartments(
            Set<String> first,
            List<String> second
    ) {
        Set<String> departments = new LinkedHashSet<>(first);
        departments.addAll(second);
        return Set.copyOf(departments);
    }

    private record OperatingHours(
            boolean known,
            boolean open,
            LocalTime openTime,
            LocalTime closeTime,
            Set<OperatingScheduleFilter> schedules
    ) {
        private static OperatingHours unknown() {
            return unknown(Set.of());
        }

        private static OperatingHours unknown(Set<OperatingScheduleFilter> schedules) {
            return new OperatingHours(false, false, null, null, schedules);
        }

        private static OperatingHours closed(Set<OperatingScheduleFilter> schedules) {
            return new OperatingHours(true, false, null, null, schedules);
        }
    }

    private record SearchKey(
            long latitude,
            long longitude,
            int radiusMeters,
            String types
    ) {
        private static SearchKey of(
                double lat,
                double lng,
                int radiusMeters,
                List<InstitutionType> requestedTypes
        ) {
            String types = requestedTypes.stream()
                    .map(Enum::name)
                    .sorted()
                    .reduce((first, second) -> first + "," + second)
                    .orElse("");
            return new SearchKey(
                    Math.round(lat * 10_000),
                    Math.round(lng * 10_000),
                    radiusMeters,
                    types
            );
        }
    }

    private record CachedSearch(List<NearbyInstitutionItemResponse> items, Instant expiresAt) {
    }

    private record OperatingHoursSearchKey(
            InstitutionType type,
            String province,
            String district,
            int dayIndex
    ) {
    }

    private record CachedPublicInstitutionLookup(
            PublicInstitutionLookup lookup,
            Instant expiresAt
    ) {
    }

    private record PublicInstitutionDetails(
            long id,
            InstitutionType type,
            String name,
            String institutionKind,
            List<String> medicalDepartments,
            String phoneNumber,
            String roadAddress,
            BigDecimal latitude,
            BigDecimal longitude,
            OperatingHours operatingHours
    ) {
    }

    private record PublicInstitutionLookup(
            List<PublicInstitutionDetails> institutions
    ) {
    }

    private record EmergencyAvailability(Integer availableBeds, String emergencyPhone) {
    }

    private record EmergencyAvailabilityLookup(
            Map<Long, EmergencyAvailability> byInstitutionId,
            Map<String, EmergencyAvailability> byInstitutionName
    ) {
    }

    private record EmergencyAvailabilitySearchKey(String province, String district) {
    }

    private record CachedEmergencyAvailability(
            EmergencyAvailabilityLookup lookup,
            Instant expiresAt
    ) {
    }
}
