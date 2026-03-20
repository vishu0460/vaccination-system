package com.vaccine.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private HttpServletRequest request;

    private TestJwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TestJwtAuthFilter(jwtService, customUserDetailsService);
    }

    @Test
    void shouldNotFilter_PublicAuthPath() {
        when(request.getRequestURI()).thenReturn("/api/auth/login");

        assertTrue(filter.exposedShouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_ProtectedAdminPath_ShouldRequireJwtProcessing() {
        when(request.getRequestURI()).thenReturn("/api/admin/dashboard/stats");

        assertFalse(filter.exposedShouldNotFilter(request));
    }

    private static class TestJwtAuthFilter extends JwtAuthFilter {
        TestJwtAuthFilter(JwtService jwtService, CustomUserDetailsService customUserDetailsService) {
            super(jwtService, customUserDetailsService);
        }

        boolean exposedShouldNotFilter(HttpServletRequest request) {
            return super.shouldNotFilter(request);
        }
    }
}
