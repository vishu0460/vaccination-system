package com.vaccine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import com.vaccine.security.JwtAuthFilter;
import com.vaccine.security.CustomUserDetailsService;
import com.vaccine.config.RateLimitFilter;
import com.vaccine.logging.RequestTracingFilter;
import com.vaccine.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5174,http://localhost:5173,http://localhost:3000}")
    private String corsAllowedOrigins;

    @Value("${app.dev.disable-auth-for-profile:false}")
    private boolean disableAuthForProfile;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter, RateLimitFilter rateLimitFilter,
                                           RequestTracingFilter requestTracingFilter,
                                           RestAuthenticationEntryPoint restAuthenticationEntryPoint) throws Exception {
        http
            .addFilterBefore(requestTracingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/auth/**", "/api/auth/**").permitAll()
                    .requestMatchers("/public/**", "/v1/public/**", "/api/public/**", "/api/v1/public/**").permitAll()
                    .requestMatchers("/health/**", "/v1/health/**", "/api/health/**", "/api/v1/health/**").permitAll()
                    .requestMatchers("/ws/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/contact", "/api/contact").permitAll()
                    .requestMatchers("/reviews/center/**", "/v1/reviews/center/**", "/api/reviews/center/**", "/api/v1/reviews/center/**").permitAll()
                    .requestMatchers("/certificates/verify/**", "/api/certificates/verify/**", "/certificate/verify/**", "/api/certificate/verify/**").permitAll()
                    .requestMatchers("/error", "/v3/api-docs/**", "/swagger-ui/**", "/h2-console/**", "/actuator/**", "/robots.txt", "/sitemap.xml").permitAll()
                    .requestMatchers("/admin/**", "/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                    .requestMatchers("/admins/**", "/api/admins/**").hasRole("SUPER_ADMIN")
                    .requestMatchers("/super-admin/**", "/api/super-admin/**", "/superadmin/**", "/api/superadmin/**").hasRole("SUPER_ADMIN");

                if (disableAuthForProfile) {
                    auth.requestMatchers(HttpMethod.GET, "/users", "/users/me", "/profile", "/profile/me", "/api/users", "/api/users/me", "/api/profile", "/api/profile/me").permitAll();
                }

                auth.requestMatchers("/user/**", "/api/user/**", "/users/**", "/api/users/**", "/profile/**", "/api/profile/**", "/v1/profile/**", "/api/v1/profile/**").authenticated()
                    .requestMatchers("/certificates/**", "/api/certificates/**").authenticated()
                    .anyRequest().authenticated();
            })
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = expandLocalOrigins(Arrays.stream(corsAllowedOrigins.split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toList()));
        if (origins.contains("*")) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOriginPatterns(origins);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> expandLocalOrigins(List<String> configuredOrigins) {
        Set<String> expandedOrigins = new LinkedHashSet<>(configuredOrigins);

        for (String origin : configuredOrigins) {
            if (!origin.startsWith("http://") && !origin.startsWith("https://")) {
                continue;
            }

            if (origin.contains("localhost")) {
                expandedOrigins.add(origin.replace("localhost", "127.0.0.1"));
            } else if (origin.contains("127.0.0.1")) {
                expandedOrigins.add(origin.replace("127.0.0.1", "localhost"));
            }
        }

        return List.copyOf(expandedOrigins);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    @Primary
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
