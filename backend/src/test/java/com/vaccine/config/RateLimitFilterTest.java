package com.vaccine.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {

    private final RateLimitFilter rateLimitFilter = new RateLimitFilter();

    @Test
    void shouldNotFilter_PublicApiPath() {
        HttpServletRequest request = request("GET", "/api/public/drives");

        assertTrue(rateLimitFilter.shouldNotFilter(request));
    }

    @Test
    void shouldFilter_ProtectedApiPath() {
        HttpServletRequest request = request("GET", "/api/admin/bookings");

        assertFalse(rateLimitFilter.shouldNotFilter(request));
    }

    @Test
    void shouldNotTreatRootAsPrefixForEveryRoute() {
        HttpServletRequest request = request("GET", "/api/user/bookings");

        assertFalse(rateLimitFilter.shouldNotFilter(request));
    }

    private HttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
