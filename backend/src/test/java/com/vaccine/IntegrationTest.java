package com.vaccine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class IntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String apiUrl(String path) {
        return "http://localhost:" + port + "/api" + path;
    }

    @Test
    void healthEndpoint_ShouldReturnUp() {
        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl("/v1/health"), String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"status\""));
    }

    @Test
    void publicEndpoints_ShouldBeAccessible() {
        // Test health first - this should always work
        ResponseEntity<String> health = restTemplate.getForEntity(apiUrl("/v1/health"), String.class);
        assertEquals(HttpStatus.OK, health.getStatusCode());
        
        // Test public drives
        ResponseEntity<String> drives = restTemplate.getForEntity(apiUrl("/public/drives"), String.class);
        assertEquals(HttpStatus.OK, drives.getStatusCode());
        
        // Test public centers
        ResponseEntity<String> centers = restTemplate.getForEntity(apiUrl("/public/centers"), String.class);
        assertEquals(HttpStatus.OK, centers.getStatusCode());
        
        // Test public summary
        ResponseEntity<String> summary = restTemplate.getForEntity(apiUrl("/public/summary"), String.class);
        assertEquals(HttpStatus.OK, summary.getStatusCode());
    }

    @Test
    void authFlow_AfterRegistration_ShouldLogin() {
        String uniqueEmail = "test" + System.currentTimeMillis() + "@test.com";
        String registerBody = String.format("""
            {
                "fullName": "Test User",
                "email": "%s",
                "password": "Test@123456",
                "dob": "2000-01-01",
                "phoneNumber": "+919876543210"
            }
            """, uniqueEmail);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> registerRequest = new HttpEntity<>(registerBody, headers);

        ResponseEntity<String> registerResponse = restTemplate.postForEntity(apiUrl("/auth/register"), registerRequest, String.class);

        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());

        String loginBody = String.format("""
            {
                "email": "%s",
                "password": "Test@123456"
            }
            """, uniqueEmail);
        HttpEntity<String> loginRequest = new HttpEntity<>(loginBody, headers);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity(apiUrl("/auth/login"), loginRequest, String.class);

        assertEquals(HttpStatus.OK, loginResponse.getStatusCode());
        assertNotNull(loginResponse.getBody());
        assertTrue(loginResponse.getBody().contains("accessToken"));
    }

    @Test
    void register_WithValidPayload_ShouldSucceed() {
        String uniqueEmail = "test" + System.currentTimeMillis() + "@test.com";
        String requestBody = String.format("""
            {
                "fullName": "Test User",
                "email": "%s",
                "password": "Test@123456",
                "dob": "2000-01-01",
                "phoneNumber": "+919876543210"
            }
            """, uniqueEmail);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl("/auth/register"), request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void protectedEndpoint_WithoutToken_ShouldBeRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl("/user/bookings"), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
