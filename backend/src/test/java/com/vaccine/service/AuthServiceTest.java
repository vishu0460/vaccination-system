package com.vaccine.service;

import com.vaccine.common.dto.*;
import com.vaccine.domain.*;
import com.vaccine.common.exception.AppException;
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
import com.vaccine.core.service.AuthService;
import com.vaccine.core.service.AuditService;
import com.vaccine.core.service.NotificationService;
import com.vaccine.core.service.OtpService;
import com.vaccine.core.service.PhoneVerificationService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doThrow;

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
    private OtpService otpService;
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
            jwtService, otpService, notificationService, auditService, phoneVerificationService
        );
        
        // Set @Value fields via reflection
        ReflectionTestUtils.setField(authService, "maxAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockMinutes", 15);
        ReflectionTestUtils.setField(authService, "includeOtpInMessages", false);
    }

@Test
    void register_WithNewEmail_ShouldCreateUser() {
        RegisterRequest req = new RegisterRequest("test@example.com", "Test User", "+1234567890", "Password123!", 25);
        
        when(userRepository.existsAnyByEmail("test@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(Role.builder().name(RoleName.USER).build()));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ReflectionTestUtils.setField(authService, "autoVerifyEmail", true);

        RegisterResponse result = authService.register(req, httpServletRequest);

        assertNotNull(result);
        assertEquals("Registration successful. You can log in now.", result.message());
        assertFalse(result.requiresVerification());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WhenOtpEmailFails_ShouldStillCreateUserAndReturnVerificationResponse() {
        RegisterRequest req = new RegisterRequest("otp@example.com", "Test User", "+1234567890", "Password123!", 25);

        when(userRepository.existsAnyByEmail("otp@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(Role.builder().name(RoleName.USER).build()));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.sendOtp(any(User.class), eq(OtpPurpose.EMAIL_VERIFICATION)))
            .thenReturn(new OtpService.OtpDispatchResult("7654321", false));
        ReflectionTestUtils.setField(authService, "autoVerifyEmail", false);
        ReflectionTestUtils.setField(authService, "includeOtpInMessages", true);

        RegisterResponse result = authService.register(req, httpServletRequest);

        assertTrue(result.requiresVerification());
        assertTrue(result.emailDeliveryFailed());
        assertEquals("7654321", result.otpPreview());
        assertTrue(result.message().contains("7654321"));
        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        RegisterRequest req = new RegisterRequest("existing@example.com", "Test User", "+1234567890", "Password123!", 25);
        
        when(userRepository.existsAnyByEmail("existing@example.com")).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> authService.register(req, httpServletRequest));
        assertEquals("Email already registered", exception.getMessage());
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        LoginRequest req = new LoginRequest("user@example.com", "password");
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
        LoginRequest req = new LoginRequest("user@example.com", "wrongpassword");
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
    void login_WithLockedAccount_ShouldThrowException() {
        LoginRequest req = new LoginRequest("user@example.com", "password");
        User user = User.builder()
            .email("user@example.com")
            .lockUntil(LocalDateTime.now().plusMinutes(30))
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        AppException exception = assertThrows(AppException.class, () -> authService.login(req, httpServletRequest));
        assertEquals("Account locked. Try again later.", exception.getMessage());
    }

    @Test
    void login_WithUnverifiedEmail_ShouldThrowException() {
        LoginRequest req = new LoginRequest("user@example.com", "password");
        User user = User.builder()
            .email("user@example.com")
            .emailVerified(false)
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        AppException exception = assertThrows(AppException.class, () -> authService.login(req, httpServletRequest));
        assertEquals("Email not verified. Please verify your email first.", exception.getMessage());
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
        User user = User.builder()
            .id(1L)
            .email("user@example.com")
            .verificationToken("validToken")
            .verificationTokenExpiry(LocalDateTime.now().plusHours(1))
            .build();

        when(userRepository.findByVerificationToken("validToken")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        ApiMessage result = authService.verifyEmail("validToken");

        assertNotNull(result);
        assertEquals("Email verified successfully", result.message());
    }

    @Test
    void verifyEmail_WithExpiredToken_ShouldThrowException() {
        User user = User.builder()
            .email("user@example.com")
            .verificationToken("expiredToken")
            .verificationTokenExpiry(LocalDateTime.now().minusHours(1))
            .build();

        when(userRepository.findByVerificationToken("expiredToken")).thenReturn(Optional.of(user));

        AppException exception = assertThrows(AppException.class, () -> authService.verifyEmail("expiredToken"));
        assertEquals("Invalid or expired verification token", exception.getMessage());
    }

    @Test
    void forgotPassword_WithValidEmail_ShouldSendResetToken() {
        User user = User.builder()
            .email("user@example.com")
            .fullName("Test User")
            .enabled(true)
            .build();
        
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ApiMessage result = authService.forgotPassword(new ForgotPasswordRequest("user@example.com"));

        assertNotNull(result);
        verify(otpService).sendOtp(user, OtpPurpose.PASSWORD_RESET);
    }

    @Test
    void forgotPassword_WithUnknownEmail_ShouldReturnGenericSuccess() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        ApiMessage result = authService.forgotPassword(new ForgotPasswordRequest("missing@example.com"));

        assertEquals("If the account exists, a password reset OTP has been sent.", result.message());
        verify(otpService, never()).sendOtp(any(User.class), any());
    }

    @Test
    void resetPassword_WithValidToken_ShouldResetPassword() {
        User user = User.builder()
            .id(1L)
            .email("user@example.com")
            .build();
        PasswordReset reset = PasswordReset.builder()
            .userEmail("user@example.com")
            .token("validToken")
            .expiresAt(LocalDateTime.now().plusHours(1))
            .used(false)
            .build();

        when(passwordResetRepository.findByTokenAndExpiresAtAfter(eq("validToken"), any(LocalDateTime.class))).thenReturn(Optional.of(reset));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordResetRepository.save(any(PasswordReset.class))).thenReturn(reset);
        when(passwordEncoder.encode("newPassword")).thenReturn("encodedPassword");

        authService.resetPassword(new ResetPasswordRequest(null, null, null, "validToken", "newPassword"));
        
        verify(passwordEncoder).encode("newPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void resetPassword_WithExpiredToken_ShouldThrowException() {
        PasswordReset reset = PasswordReset.builder()
            .userEmail("user@example.com")
            .token("expiredToken")
            .expiresAt(LocalDateTime.now().minusHours(1))
            .used(false)
            .build();

        when(passwordResetRepository.findByTokenAndExpiresAtAfter(eq("expiredToken"), any(LocalDateTime.class))).thenReturn(Optional.of(reset));

        AppException exception = assertThrows(AppException.class, () -> {
            authService.resetPassword(new ResetPasswordRequest(null, null, null, "expiredToken", "newPassword"));
        });
        assertEquals("Invalid or expired reset token", exception.getMessage());
    }

    @Test
    void resetPassword_WithValidOtp_ShouldResetPassword() {
        User user = User.builder()
            .email("user@example.com")
            .otpExpiry(LocalDateTime.now().plusMinutes(5))
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Password123!")).thenReturn("encodedPassword");

        authService.resetPassword(new ResetPasswordRequest("user@example.com", "1234567", "Password123!", null, null));

        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(user);
        verify(otpService).verifyOtp(user, "1234567", OtpPurpose.PASSWORD_RESET, false);
    }
}
