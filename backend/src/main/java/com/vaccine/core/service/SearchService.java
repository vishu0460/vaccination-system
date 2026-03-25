package com.vaccine.core.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaccine.common.dto.*;
import com.vaccine.domain.SearchLog;
import com.vaccine.domain.Slot;
import com.vaccine.domain.Status;
import com.vaccine.domain.VaccinationCenter;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.infrastructure.persistence.repository.SearchLogRepository;
import com.vaccine.infrastructure.persistence.repository.SlotRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationCenterRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import com.vaccine.util.SlotStatusResolver;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    private static final int MAX_QUERY_LENGTH = 80;
    private static final List<Status> BOOKABLE_DRIVE_STATUSES = List.of(Status.UPCOMING, Status.LIVE);

    private final VaccinationCenterRepository centerRepository;
    private final VaccinationDriveRepository driveRepository;
    private final SlotRepository slotRepository;
    private final SearchLogRepository searchLogRepository;
    private final LocationService locationService;
    private final ObjectMapper objectMapper;

    @Value("classpath:search/india-cities.json")
    private Resource citiesResource;

    private List<CityReference> indiaCities = List.of();

    @PostConstruct
    void loadCities() {
        try (InputStream inputStream = citiesResource.getInputStream()) {
            List<CitySeed> seeds = objectMapper.readValue(inputStream, new TypeReference<>() {});
            Map<String, CityReference> unique = new LinkedHashMap<>();

            seeds.stream()
                .map(seed -> new CityReference(cleanDisplay(seed.name()), cleanDisplay(seed.state())))
                .filter(city -> city.name() != null)
                .forEach(city -> unique.putIfAbsent(city.name().toLowerCase(Locale.ROOT), city));

            indiaCities = List.copyOf(unique.values());
        } catch (Exception error) {
            log.error("Failed to load India city dataset", error);
            indiaCities = List.of();
        }
    }

    @Cacheable(value = "public-cities", key = "T(java.lang.String).valueOf(#query) + ':' + #limit")
    public List<String> getCitySuggestions(String query, int limit) {
        String sanitizedQuery = sanitizeQuery(query);
        return rankCities(sanitizedQuery, limit).stream()
            .map(SearchCityResult::name)
            .toList();
    }

    @Transactional
    public SmartSearchResponse search(String query, String cityFilter, int limit) {
        String sanitizedQuery = sanitizeQuery(query);
        String sanitizedCity = sanitizeQuery(cityFilter);
        int resolvedLimit = Math.max(1, Math.min(limit, 10));

        List<SearchCityResult> cityResults = rankCities(sanitizedQuery, resolvedLimit);
        List<SearchCenterResult> centerResults = rankCenters(sanitizedQuery, sanitizedCity, null, null, resolvedLimit);
        List<SearchDriveResult> driveResults = rankDrives(sanitizedQuery, sanitizedCity, resolvedLimit);
        String didYouMean = suggestCityCorrection(sanitizedQuery, cityResults);

        int totalResults = cityResults.size() + centerResults.size() + driveResults.size();
        logSearch(sanitizedQuery, sanitizedCity, null, "smart-search", totalResults);

        return new SmartSearchResponse(
            sanitizedQuery == null ? "" : query == null ? "" : query.trim(),
            sanitizedQuery == null ? "" : sanitizedQuery,
            didYouMean,
            sanitizedCity,
            null,
            cityResults,
            centerResults,
            driveResults,
            totalResults
        );
    }

    @Transactional
    public NearbyCentersResponse findNearbyCenters(double lat, double lng, int limit) {
        int resolvedLimit = Math.max(1, Math.min(limit, 8));
        String detectedCity = locationService.reverseGeocode(lat, lng).orElse(null);
        List<SearchCenterResult> centers = rankCenters(null, detectedCity, lat, lng, resolvedLimit);
        if (detectedCity == null && !centers.isEmpty()) {
            detectedCity = centers.get(0).city();
        }

        logSearch("nearby centers", detectedCity, detectedCity, "nearby-centers", centers.size());
        return new NearbyCentersResponse(detectedCity, detectedCity != null, centers);
    }

    public SearchAnalyticsResponse getSearchAnalytics() {
        LocalDateTime since = LocalDate.now().minusDays(29).atStartOfDay();
        List<SearchLog> logs = searchLogRepository.findBySearchedAtAfter(since);

        List<SearchMetricResponse> topCities = logs.stream()
            .map(log -> firstNonBlank(log.getCity(), log.getDetectedCity()))
            .filter(Objects::nonNull)
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(6)
            .map(entry -> new SearchMetricResponse(entry.getKey(), entry.getValue()))
            .toList();

        List<SearchMetricResponse> topKeywords = logs.stream()
            .map(SearchLog::getNormalizedQuery)
            .filter(value -> value != null && !value.isBlank() && !"nearby centers".equals(value))
            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(6)
            .map(entry -> new SearchMetricResponse(entry.getKey(), entry.getValue()))
            .toList();

        List<SearchTrendPointResponse> trends = logs.stream()
            .collect(Collectors.groupingBy(log -> log.getSearchedAt().toLocalDate(), TreeMap::new, Collectors.counting()))
            .entrySet().stream()
            .map(entry -> new SearchTrendPointResponse(entry.getKey(), entry.getValue()))
            .toList();

        return new SearchAnalyticsResponse(logs.size(), topCities, topKeywords, trends);
    }

    private List<SearchCityResult> rankCities(String query, int limit) {
        String normalizedQuery = normalize(query);
        Set<String> dbCities = new LinkedHashSet<>(centerRepository.findDistinctCities());

        Map<String, CityReference> merged = new LinkedHashMap<>();
        indiaCities.forEach(city -> merged.putIfAbsent(normalize(city.name()), city));
        dbCities.stream()
            .filter(Objects::nonNull)
            .map(String::trim)
            .filter(value -> !value.isBlank())
            .forEach(city -> merged.put(normalize(city), new CityReference(cleanDisplay(city), null)));

        return merged.values().stream()
            .map(city -> scoreCity(city, normalizedQuery))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(SearchCityResult::score).reversed().thenComparing(SearchCityResult::name, String.CASE_INSENSITIVE_ORDER))
            .limit(limit)
            .toList();
    }

    private List<SearchCenterResult> rankCenters(String query, String cityFilter, Double userLat, Double userLng, int limit) {
        String normalizedQuery = normalize(query);
        String normalizedCity = normalize(cityFilter);
        List<VaccinationCenter> candidateCenters = (userLat != null && userLng != null)
            ? centerRepository.findAll()
            : centerRepository.searchPublicCenters(firstNonBlank(normalizedCity, normalizedQuery), PageRequest.of(0, Math.max(limit * 3, 12))).getContent();

        return candidateCenters.stream()
            .filter(center -> matchesCityFilter(center, normalizedCity))
            .map(center -> buildCenterResult(center, normalizedQuery, userLat, userLng))
            .filter(Objects::nonNull)
            .sorted(Comparator
                .comparing(SearchCenterResult::score).reversed()
                .thenComparing(result -> result.distanceKm() == null ? Double.MAX_VALUE : result.distanceKm()))
            .limit(limit)
            .toList();
    }

    private List<SearchDriveResult> rankDrives(String query, String cityFilter, int limit) {
        String normalizedQuery = normalize(query);
        String normalizedCity = normalize(cityFilter);

        return driveRepository.findVisibleDrives(BOOKABLE_DRIVE_STATUSES, normalizedCity, LocalDate.now(), null).stream()
            .map(drive -> buildDriveResult(drive, normalizedQuery))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(SearchDriveResult::score).reversed().thenComparing(SearchDriveResult::driveDate))
            .limit(limit)
            .toList();
    }

    private SearchCenterResult buildCenterResult(VaccinationCenter center, String query, Double userLat, Double userLng) {
        if (center == null) {
            return null;
        }

        MatchScore score = firstNonNull(
            scoreTextMatch(center.getName(), query),
            scoreTextMatch(center.getCity(), query),
            scoreTextMatch(center.getAddress(), query)
        );

        if (query != null && score == null) {
            return null;
        }

        Double distance = resolveDistance(center, userLat, userLng);
        double finalScore = score != null ? score.score() : 62d;
        if (distance != null) {
            finalScore += Math.max(0, 22 - Math.min(distance, 22));
        }

        return new SearchCenterResult(
            center.getId(),
            cleanDisplay(center.getName()),
            cleanDisplay(center.getCity()),
            cleanDisplay(center.getState()),
            cleanDisplay(center.getAddress()),
            distance == null ? null : round(distance),
            round(finalScore),
            score == null ? "city" : score.matchType()
        );
    }

    private SearchDriveResult buildDriveResult(VaccinationDrive drive, String query) {
        if (drive == null) {
            return null;
        }

        MatchScore score = firstNonNull(
            scoreTextMatch(drive.getTitle(), query),
            scoreTextMatch(drive.getCenter() != null ? drive.getCenter().getCity() : null, query),
            scoreTextMatch(drive.getCenter() != null ? drive.getCenter().getName() : null, query),
            scoreTextMatch(drive.getVaccineType(), query)
        );

        if (query != null && score == null) {
            return null;
        }

        return new SearchDriveResult(
            drive.getId(),
            cleanDisplay(drive.getTitle()),
            cleanDisplay(drive.getCenter() != null ? drive.getCenter().getCity() : null),
            cleanDisplay(drive.getCenter() != null ? drive.getCenter().getName() : null),
            drive.getDriveDate(),
            cleanDisplay(drive.getVaccineType()),
            getAvailableSlots(drive.getId()),
            round(score == null ? 60d : score.score()),
            score == null ? "city" : score.matchType()
        );
    }

    private SearchCityResult scoreCity(CityReference city, String query) {
        if (city == null || city.name() == null) {
            return null;
        }

        if (query == null) {
            return new SearchCityResult(city.name(), city.state(), 50d, "popular");
        }

        MatchScore score = scoreTextMatch(city.name(), query);
        if (score == null && city.state() != null) {
            score = scoreTextMatch(city.state(), query);
        }

        if (score == null) {
            return null;
        }

        return new SearchCityResult(city.name(), city.state(), round(score.score()), score.matchType());
    }

    private String suggestCityCorrection(String query, List<SearchCityResult> cityResults) {
        if (query == null || query.length() < 3 || cityResults.isEmpty()) {
            return null;
        }

        SearchCityResult best = cityResults.get(0);
        String normalizedBest = normalize(best.name());
        if (query.equals(normalizedBest)) {
            return null;
        }

        MatchScore fuzzyScore = scoreTextMatch(best.name(), query);
        return fuzzyScore != null && "fuzzy".equals(fuzzyScore.matchType()) ? best.name() : null;
    }

    private MatchScore scoreTextMatch(String candidate, String query) {
        String normalizedCandidate = normalize(candidate);
        if (normalizedCandidate == null) {
            return null;
        }

        if (query == null) {
            return new MatchScore(50d, "popular");
        }

        if (normalizedCandidate.equals(query)) {
            return new MatchScore(120d, "exact");
        }
        if (normalizedCandidate.startsWith(query)) {
            return new MatchScore(104d, "prefix");
        }
        if (Arrays.stream(normalizedCandidate.split(" ")).anyMatch(part -> part.startsWith(query))) {
            return new MatchScore(94d, "word-prefix");
        }
        if (normalizedCandidate.contains(query)) {
            return new MatchScore(82d, "contains");
        }

        int distance = levenshtein(normalizedCandidate, query);
        if (query.length() >= 3 && distance <= 2) {
            double score = 72d - (distance * 8d);
            if (normalizedCandidate.charAt(0) == query.charAt(0)) {
                score += 4d;
            }
            if (normalizedCandidate.length() >= 2 && query.length() >= 2
                && normalizedCandidate.substring(0, 2).equals(query.substring(0, 2))) {
                score += 6d;
            }
            return new MatchScore(score, "fuzzy");
        }

        return null;
    }

    private boolean matchesCityFilter(VaccinationCenter center, String cityFilter) {
        if (cityFilter == null) {
            return true;
        }

        String normalizedCity = normalize(center.getCity());
        return normalizedCity != null
            && (normalizedCity.contains(cityFilter)
            || cityFilter.contains(normalizedCity)
            || levenshtein(normalizedCity, cityFilter) <= 3);
    }

    private Double resolveDistance(VaccinationCenter center, Double userLat, Double userLng) {
        if (userLat == null || userLng == null || center == null) {
            return null;
        }

        if (center.getLat() == null || center.getLng() == null) {
            enrichCenterCoordinates(center);
        }

        if (center.getLat() == null || center.getLng() == null) {
            return null;
        }

        return haversine(userLat, userLng, center.getLat(), center.getLng());
    }

    private void enrichCenterCoordinates(VaccinationCenter center) {
        if (center.getLat() != null && center.getLng() != null) {
            return;
        }

        locationService.resolveCityCoordinates(center.getCity(), center.getState()).ifPresent(point -> {
            center.setLat(point.lat());
            center.setLng(point.lng());
            centerRepository.save(center);
        });
    }

    private void logSearch(String query, String city, String detectedCity, String source, int resultCount) {
        String safeQuery = firstNonBlank(query, "nearby centers");
        searchLogRepository.save(SearchLog.builder()
            .query(safeQuery)
            .normalizedQuery(normalize(safeQuery))
            .city(city)
            .detectedCity(detectedCity)
            .source(source)
            .resultCount(resultCount)
            .searchedAt(LocalDateTime.now())
            .build());
    }

    private long getAvailableSlots(Long driveId) {
        return slotRepository.findByDrive_IdOrderByDateTimeAsc(driveId).stream()
            .filter(slot -> SlotStatusResolver.resolve(slot) != com.vaccine.domain.SlotStatus.EXPIRED)
            .mapToLong(this::getAvailableCapacity)
            .sum();
    }

    private long getAvailableCapacity(Slot slot) {
        int capacity = slot.getCapacity() == null ? 0 : slot.getCapacity();
        int bookedCount = slot.getBookedCount() == null ? 0 : slot.getBookedCount();
        return Math.max(0, capacity - bookedCount);
    }

    private String sanitizeQuery(String value) {
        if (value == null) {
            return null;
        }

        String sanitized = value
            .replaceAll("[\\p{Cntrl}]+", " ")
            .replaceAll("[^\\p{L}\\p{N}\\s,.'-]", " ")
            .replaceAll("\\s+", " ")
            .trim();

        if (sanitized.isBlank()) {
            return null;
        }

        return sanitized.substring(0, Math.min(sanitized.length(), MAX_QUERY_LENGTH));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String cleanDisplay(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private MatchScore firstNonNull(MatchScore... matches) {
        for (MatchScore match : matches) {
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double originLat = Math.toRadians(lat1);
        double destinationLat = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(originLat) * Math.cos(destinationLat)
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadiusKm * c;
    }

    private int levenshtein(String left, String right) {
        int[] costs = new int[right.length() + 1];
        for (int j = 0; j < costs.length; j++) {
            costs[j] = j;
        }

        for (int i = 1; i <= left.length(); i++) {
            costs[0] = i;
            int corner = i - 1;
            for (int j = 1; j <= right.length(); j++) {
                int upper = costs[j];
                int replacementCost = left.charAt(i - 1) == right.charAt(j - 1) ? corner : corner + 1;
                costs[j] = Math.min(Math.min(costs[j - 1] + 1, upper + 1), replacementCost);
                corner = upper;
            }
        }

        return costs[right.length()];
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private record MatchScore(double score, String matchType) {
    }

    private record CityReference(String name, String state) {
    }

    private record CitySeed(String id, String name, String state) {
    }
}
