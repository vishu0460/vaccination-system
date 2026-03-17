package com.vaccine.service;

import com.vaccine.common.dto.*;
import com.vaccine.domain.*;
import com.vaccine.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.*;
import com.vaccine.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private EmailVerificationRepository emailVerificationRepository;
    @Mock
    private PasswordResetRepository passwordResetRepository;
    @Mock
    private PhoneVerificationRepository phoneVerificationRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditService auditService;
    @Mock
    private PhoneVerificationService phoneVerificationService;
    @Mock
    private HttpServletRequest httpServletRequest;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
            userRepository, roleRepository, emailVerificationRepository,
            passwordResetRepository, phoneVerificationRepository, authenticationManager, passwordEncoder,
            jwtService, notificationService, auditService, phoneVerificationService
        );
        
        // Set @Value fields via reflection
        ReflectionTestUtils.setField(authService, "maxAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockMinutes", 15);
    }

    @Test
    void register_WithNewEmail_ShouldCreateUser() {
        RegisterRequest req = new RegisterRequest("test@example.com", "Test User", "Password123", 25, "+1234567890", "123 Main St", "City", "State", "12345");
        
        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(Role.builder().name(RoleName.USER).build()));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiMessage result = authService.register(req, httpServletRequest);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        RegisterRequest req = new RegisterRequest("existing@example.com", "Test User", "Password123", 25, "+1234567890", "123 Main St", "City", "State", "12345");
        
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(AppException.class, () -> authService.register(req, httpServletRequest));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        LoginRequest req = new LoginRequest("user@example.com", "password", null);
        User user = User.builder()
            .email("user@example.com")
            .emailVerified(true)
            .failedLoginAttempts(0)
            .roles(Set.of(Role.builder().name(RoleName.USER).build()))
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        Authentication auth = new UsernamePasswordAuthenticationToken("user@example.com", "password");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtService.createAccessToken(anyString(), anyMap())).thenReturn("accessToken");
        when(jwtService.createRefreshToken(anyString())).thenReturn("refreshToken");
        when(jwtService.accessExpirySeconds()).thenReturn(900L);

        AuthResponse result = authService.login(req, httpServletRequest);

        assertNotNull(result);
        assertEquals("accessToken", result.accessToken());
        assertEquals("refreshToken", result.refreshToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void login_WithInvalidCredentials_ShouldThrowException() {
        LoginRequest req = new LoginRequest("user@example.com", "wrongpassword", null);
        User user = User.builder()
            .email("user@example.com")
            .emailVerified(true)
            .failedLoginAttempts(0)
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        doThrow(new BadCredentialsException("Invalid")).when(authenticationManager).authenticate(any());

        assertThrows(BadCredentialsException.class, () -> authService.login(req, httpServletRequest));
    }

    @Test
    void login_WithUnverifiedEmail_ShouldThrowException() {
        LoginRequest req = new LoginRequest("user@example.com", "password", null);
        User user = User.builder()
            .email("user@example.com")
            .emailVerified(false)
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(AppException.class, () -> authService.login(req, httpServletRequest));
    }

    @Test
    void login_WithLockedAccount_ShouldThrowException() {
        LoginRequest req = new LoginRequest("user@example.com", "password", null);
        User user = User.builder()
            .email("user@example.com")
            .lockUntil(LocalDateTime.now().plusMinutes(30))
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(AppException.class, () -> authService.login(req, httpServletRequest));
    }

    @Test
    void refresh_WithValidToken_ShouldReturnNewAuthResponse() {
        RefreshTokenRequest req = new RefreshTokenRequest("refreshToken");
        User user = User.builder()
            .email("user@example.com")
            .roles(Set.of(Role.builder().name(RoleName.USER).build()))
            .build();

        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(claims.getSubject()).thenReturn("user@example.com");
        when(claims.get("type")).thenReturn("refresh");
        when(jwtService.parse("refreshToken")).thenReturn(claims);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(jwtService.createAccessToken(anyString(), anyMap())).thenReturn("newAccessToken");
        when(jwtService.createRefreshToken(anyString())).thenReturn("newRefreshToken");
        when(jwtService.accessExpirySeconds()).thenReturn(900L);

        AuthResponse result = authService.refresh(req);

        assertNotNull(result);
        assertEquals("newAccessToken", result.accessToken());
    }

    @Test
    void verifyEmail_WithValidToken_ShouldVerifyEmail() {
        EmailVerification verification = EmailVerification.builder()
            .user(User.builder().email("user@example.com").emailVerified(false).build())
            .token("validToken")
            .expiresAt(LocalDateTime.now().plusHours(1))
            .used(false)
            .build();

        when(emailVerificationRepository.findByToken("validToken")).thenReturn(Optional.of(verification));
        when(emailVerificationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApiMessage result = authService.verifyEmail("validToken");

        assertNotNull(result);
        assertEquals("Email verified successfully", result.message());
    }

    @Test
    void verifyEmail_WithExpiredToken_ShouldThrowException() {
        EmailVerification verification = EmailVerification.builder()
            .token("expiredToken")
            .expiresAt(LocalDateTime.now().minusHours(1))
            .used(false)
            .build();

        when(emailVerificationRepository.findByToken("expiredToken")).thenReturn(Optional.of(verification));

        assertThrows(AppException.class, () -> authService.verifyEmail("expiredToken"));
    }

    @Test
    void forgotPassword_WithValidEmail_ShouldSendResetToken() {
        User user = User.builder().email("user@example.com").build();
        
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ApiMessage result = authService.forgotPassword(new ForgotPasswordRequest("user@example.com"));

        assertNotNull(result);
        verify(notificationService).sendEmail(any(User.class), eq("Password Reset"), anyString());
    }

    @Test
    void resetPassword_WithValidToken_ShouldResetPassword() {
        User user = User.builder().email("user@example.com").build();
        PasswordReset reset = PasswordReset.builder()
            .user(user)
            .token("validToken")
            .expiresAt(LocalDateTime.now().plusHours(1))
            .used(false)
            .build();

        when(passwordResetRepository.findByToken("validToken")).thenReturn(Optional.of(reset));

        authService.resetPassword(new ResetPasswordRequest("validToken", "newPassword"));
        
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldThrowException() {
        User user = User.builder().email("user@example.com").build();
        PasswordReset reset = PasswordReset.builder()
            .user(user)
            .token("expiredToken")
            .expiresAt(LocalDateTime.now().minusHours(1))
            .used(false)
            .build();

        when(passwordResetRepository.findByToken("expiredToken")).thenReturn(Optional.of(reset));

        AppException exception = assertThrows(AppException.class, () -> {
            authService.resetPassword(new ResetPasswordRequest("expiredToken", "newPassword"));
        });
        assertEquals("Token expired or used", exception.getMessage());
    }
}

