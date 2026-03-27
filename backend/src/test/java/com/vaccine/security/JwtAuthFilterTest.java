package com.vaccine.security;

import com.vaccine.infrastructure.persistence.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import org.springframework.http.HttpHeaders;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    private TestJwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TestJwtAuthFilter(jwtService, customUserDetailsService, userRepository);
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

    @Test
    void shouldNotFilter_ContactPostWithoutToken() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/contact");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        assertTrue(filter.exposedShouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_ContactPostWithToken_ShouldRequireJwtProcessing() {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/contact");
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer sample-token");

        assertFalse(filter.exposedShouldNotFilter(request));
    }

    private static class TestJwtAuthFilter extends JwtAuthFilter {
        TestJwtAuthFilter(JwtService jwtService, CustomUserDetailsService customUserDetailsService, UserRepository userRepository) {
            super(jwtService, customUserDetailsService, userRepository);
        }

        boolean exposedShouldNotFilter(HttpServletRequest request) {
            return super.shouldNotFilter(request);
        }
    }
}
