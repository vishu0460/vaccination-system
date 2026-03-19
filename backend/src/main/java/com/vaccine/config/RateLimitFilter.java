package com.vaccine.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, ClientRateLimiter> rateLimiters = new ConcurrentHashMap<>();

    @Value("${security.rate-limit.requests-per-minute:600}")
    private int requestsPerMinute;

    private static class ClientRateLimiter {
        private final AtomicInteger requests = new AtomicInteger(0);
        private volatile long windowStart = System.currentTimeMillis();
        private final int requestsPerMinute;

        public ClientRateLimiter(int requestsPerMinute) {
            this.requestsPerMinute = requestsPerMinute;
        }

        public boolean isAllowed() {
            long now = System.currentTimeMillis();
            if (now - windowStart > TimeUnit.MINUTES.toMillis(1)) {
                requests.set(0);
                windowStart = now;
            }
            return requests.incrementAndGet() <= requestsPerMinute;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIpAddress(request);
        ClientRateLimiter limiter = rateLimiters.computeIfAbsent(clientIp, k -> new ClientRateLimiter(requestsPerMinute));

        if (!limiter.isAllowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Rate limit exceeded. Too many requests from this IP. Try again later.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        String[] publicPaths = {
            "/health", "/api/health", "/api/health/",
            "/auth/", "/api/auth/",
            "/public/", "/api/public/",
            "/api/contact/", "/api/news/", "/api/reviews/", 
            "/api/feedback/",
            "/certificates/verify/",
            "/v3/api-docs/", "/swagger-ui/", "/h2-console/",
            "/error",
            "/", "/robots.txt", "/sitemap.xml",
            "/actuator/"
        };
        
        for (String publicPath : publicPaths) {
            if (path.equals(publicPath) || path.startsWith(publicPath)) {
                return true;
            }
        }
        
        return false;
    }
}
