package com.vaccine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaccine.common.dto.*;
import com.vaccine.security.JwtAuthFilter;
import com.vaccine.security.JwtService;
import com.vaccine.config.RateLimitFilter;
import com.vaccine.security.RestAuthenticationEntryPoint;
import com.vaccine.core.service.AuthService;
import com.vaccine.exception.GlobalExceptionHandler;
import com.vaccine.web.controller.AuthController;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {"server.servlet.context-path=/api"})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private JwtAuthFilter jwtAuthFilter;

    @MockBean
    private RateLimitFilter rateLimitFilter;

    @MockBean
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    // ✅ REGISTER TEST
    @Test
    void register_WithValidRequest_ShouldReturnSuccess() throws Exception {
        RegisterRequest req = new RegisterRequest(
                "test@example.com",
                "Test User",
                "Password123",
                25,
                "+1234567890"
        );

        when(authService.register(any(), any()))
                .thenReturn(new ApiMessage("Registered successfully"));

        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registered successfully"));
    }

    // ✅ LOGIN SUCCESS
    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() throws Exception {
        LoginRequest loginReq = new LoginRequest("user@example.com", "password");

        AuthResponse response = new AuthResponse(
                "accessToken",
                "refreshToken",
                "Bearer",
                900L,
                "user@example.com",
                "USER"
        );

        when(authService.login(any(), any()))
                .thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("accessToken"))
                .andExpect(jsonPath("$.refreshToken").value("refreshToken"));
    }

    // ✅ LOGIN FAILURE (FIXED)
    @Test
    void login_WithInvalidCredentials_ShouldReturnBadRequest() throws Exception {
        LoginRequest loginReq = new LoginRequest("user@example.com", "wrongpassword");

        when(authService.login(any(), any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isBadRequest());
    }

    // ✅ REFRESH TOKEN
    @Test
    void refresh_WithValidToken_ShouldReturnAuthResponse() throws Exception {
        RefreshTokenRequest refreshReq = new RefreshTokenRequest("refreshToken");

        AuthResponse response = new AuthResponse(
                "newAccessToken",
                "newRefreshToken",
                "Bearer",
                900L,
                "user@example.com",
                "USER"
        );

        when(authService.refresh(any()))
                .thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"));
    }

    // ✅ FORGOT PASSWORD
    @Test
    void forgotPassword_WithValidEmail_ShouldReturnSuccess() throws Exception {
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest("user@example.com");

        when(authService.forgotPassword(any()))
                .thenReturn(new ApiMessage("Password reset token sent"));

        mockMvc.perform(post("/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(forgotReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset token sent"));
    }

    // ✅ RESET PASSWORD (FIXED FOR VOID METHOD)
    @Test
    void resetPassword_WithValidToken_ShouldReturnSuccess() throws Exception {
        ResetPasswordRequest resetReq = new ResetPasswordRequest("token", "newPassword123");

        doNothing().when(authService).resetPassword(any());

        mockMvc.perform(post("/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk());
    }
}