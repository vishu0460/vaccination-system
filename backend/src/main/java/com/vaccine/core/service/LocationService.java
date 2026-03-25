package com.vaccine.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationService {
    private static final String USER_AGENT = "VaxZone Search/1.0";

    private final ObjectMapper objectMapper;

    @Value("${app.search.opencage.api-key:}")
    private String openCageApiKey;

    @Cacheable(value = "geo-city", key = "#city + '|' + T(java.lang.String).valueOf(#state)")
    public Optional<GeoPoint> resolveCityCoordinates(String city, String state) {
        String query = joinLocation(city, state);
        if (query == null) {
            return Optional.empty();
        }

        return hasOpenCageKey()
            ? lookupWithOpenCage(query)
            : lookupWithNominatim(query);
    }

    @Cacheable(value = "geo-reverse", key = "#lat + '|' + #lng")
    public Optional<String> reverseGeocode(double lat, double lng) {
        return hasOpenCageKey()
            ? reverseWithOpenCage(lat, lng)
            : reverseWithNominatim(lat, lng);
    }

    private Optional<GeoPoint> lookupWithOpenCage(String query) {
        String url = UriComponentsBuilder
            .fromHttpUrl("https://api.opencagedata.com/geocode/v1/json")
            .queryParam("q", query + ", India")
            .queryParam("key", openCageApiKey)
            .queryParam("limit", 1)
            .queryParam("countrycode", "in")
            .build()
            .toUriString();

        return executeGeoPointRequest(url);
    }

    private Optional<GeoPoint> lookupWithNominatim(String query) {
        String url = UriComponentsBuilder
            .fromHttpUrl("https://nominatim.openstreetmap.org/search")
            .queryParam("q", query + ", India")
            .queryParam("format", "jsonv2")
            .queryParam("countrycodes", "in")
            .queryParam("limit", 1)
            .build()
            .toUriString();

        return executeNominatimSearch(url);
    }

    private Optional<String> reverseWithOpenCage(double lat, double lng) {
        String url = UriComponentsBuilder
            .fromHttpUrl("https://api.opencagedata.com/geocode/v1/json")
            .queryParam("q", lat + "," + lng)
            .queryParam("key", openCageApiKey)
            .queryParam("limit", 1)
            .queryParam("countrycode", "in")
            .build()
            .toUriString();

        try {
            String response = restClient().get().uri(url).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode components = root.path("results").path(0).path("components");
            String city = firstNonBlank(
                components.path("city").asText(null),
                components.path("town").asText(null),
                components.path("state_district").asText(null),
                components.path("county").asText(null)
            );
            return Optional.ofNullable(blankToNull(city));
        } catch (Exception error) {
            log.debug("Reverse geocoding with OpenCage failed: {}", error.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> reverseWithNominatim(double lat, double lng) {
        String url = UriComponentsBuilder
            .fromHttpUrl("https://nominatim.openstreetmap.org/reverse")
            .queryParam("lat", lat)
            .queryParam("lon", lng)
            .queryParam("format", "jsonv2")
            .build()
            .toUriString();

        try {
            String response = restClient().get().uri(url).retrieve().body(String.class);
            JsonNode address = objectMapper.readTree(response).path("address");
            String city = firstNonBlank(
                address.path("city").asText(null),
                address.path("town").asText(null),
                address.path("village").asText(null),
                address.path("county").asText(null)
            );
            return Optional.ofNullable(blankToNull(city));
        } catch (Exception error) {
            log.debug("Reverse geocoding with Nominatim failed: {}", error.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GeoPoint> executeGeoPointRequest(String url) {
        try {
            String response = restClient().get().uri(url).retrieve().body(String.class);
            JsonNode geometry = objectMapper.readTree(response).path("results").path(0).path("geometry");
            if (geometry.isMissingNode()) {
                return Optional.empty();
            }

            return Optional.of(new GeoPoint(
                geometry.path("lat").asDouble(),
                geometry.path("lng").asDouble()
            ));
        } catch (Exception error) {
            log.debug("OpenCage coordinate lookup failed: {}", error.getMessage());
            return Optional.empty();
        }
    }

    private Optional<GeoPoint> executeNominatimSearch(String url) {
        try {
            String response = restClient().get().uri(url).retrieve().body(String.class);
            JsonNode node = objectMapper.readTree(response).path(0);
            if (node.isMissingNode()) {
                return Optional.empty();
            }

            return Optional.of(new GeoPoint(
                node.path("lat").asDouble(),
                node.path("lon").asDouble()
            ));
        } catch (Exception error) {
            log.debug("Nominatim coordinate lookup failed: {}", error.getMessage());
            return Optional.empty();
        }
    }

    private RestClient restClient() {
        return RestClient.builder()
            .defaultHeader("User-Agent", USER_AGENT)
            .defaultHeader("Accept", "application/json")
            .build();
    }

    private boolean hasOpenCageKey() {
        return openCageApiKey != null && !openCageApiKey.isBlank();
    }

    private String joinLocation(String city, String state) {
        String cleanCity = blankToNull(city);
        String cleanState = blankToNull(state);

        if (cleanCity == null && cleanState == null) {
            return null;
        }

        if (cleanCity == null) {
            return cleanState;
        }

        return cleanState == null ? cleanCity : cleanCity + ", " + cleanState;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String cleaned = blankToNull(value);
            if (cleaned != null) {
                return cleaned;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record GeoPoint(double lat, double lng) {
    }
}
