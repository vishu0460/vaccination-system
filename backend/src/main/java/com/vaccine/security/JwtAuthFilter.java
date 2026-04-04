package com.vaccine.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.logging.LogContextKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(2)
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService,
                         CustomUserDetailsService customUserDetailsService,
                         UserRepository userRepository) {
        this.jwtService = jwtService;
        this.customUserDetailsService = customUserDetailsService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        String method = request.getMethod();

        String[] exactPublicPaths = {
            "/",
            "/error",
            "/robots.txt",
            "/sitemap.xml",
            "/health",
            "/v1/health",
            "/api/health",
            "/api/v1/health"
        };

        for (String exactPublicPath : exactPublicPaths) {
            if (path.equals(exactPublicPath)) {
                return true;
            }
        }

        boolean isContactPost = "POST".equalsIgnoreCase(method) && ("/contact".equals(path) || "/api/contact".equals(path));
        boolean isContactOptions = "OPTIONS".equalsIgnoreCase(method) && (path.startsWith("/contact") || path.startsWith("/api/contact"));
        if (isContactPost) {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            // Keep anonymous contact submissions public, but honor JWT context when token is present.
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return true;
            }
        }
        if (isContactOptions) {
            return true;
        }

        String[] publicPathPrefixes = {
            "/actuator/",
            "/auth/",
            "/api/auth/",
            "/public/",
            "/v1/public/",
            "/api/public/",
            "/api/v1/public/",
            "/reviews/center/",
            "/v1/reviews/center/",
            "/api/reviews/center/",
            "/api/v1/reviews/center/",
            "/certificates/verify/",
            "/api/certificates/verify/",
            "/certificate/verify/",
            "/api/certificate/verify/",
            "/v3/api-docs/",
            "/swagger-ui/",
            "/h2-console/"
        };

        for (String publicPathPrefix : publicPathPrefixes) {
            if (path.startsWith(publicPathPrefix)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        log.info("JWT filter processing method={} path={} hasAuthorization={}",
            request.getMethod(), request.getRequestURI(), authHeader != null && !authHeader.isBlank());

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {
                log.debug("JWT header missing for {} {}", request.getMethod(), request.getRequestURI());
            }
            filterChain.doFilter(request, response);
            return;
        }

        try {

            String token = authHeader.substring(7);
            Claims claims = jwtService.parse(token);
            String email = claims.getSubject();

            if (email != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                UserDetails userDetails =
                        customUserDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    userRepository.findIdByEmail(email)
                        .ifPresent(userId -> MDC.put(LogContextKeys.USER_ID, String.valueOf(userId)));
                    MDC.put(LogContextKeys.USER_EMAIL, email);
                    log.info("JWT authenticated principal={} path={}", email, request.getRequestURI());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities());

                    authentication.setDetails(
                            new WebAuthenticationDetailsSource()
                                    .buildDetails(request));

                    SecurityContextHolder.getContext()
                            .setAuthentication(authentication);
                } else {
                    log.warn("JWT token invalid for principal={} path={}", email, request.getRequestURI());
                }
            }

        } catch (Exception ex) {

            log.warn("JWT authentication failed for path={}: {}", request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
