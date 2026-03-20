package com.vaccine.service;

import com.vaccine.common.dto.ApiMessage;
import com.vaccine.common.dto.FeedbackRequest;
import com.vaccine.common.dto.FeedbackResponse;
import com.vaccine.core.service.CommunicationNotificationService;
import com.vaccine.domain.Feedback;
import com.vaccine.domain.User;
import com.vaccine.common.exception.AppException;
import com.vaccine.infrastructure.persistence.repository.FeedbackRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.vaccine.core.service.FeedbackService;
import com.vaccine.domain.FeedbackStatus;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private CommunicationNotificationService communicationNotificationService;

    @InjectMocks
    private FeedbackService feedbackService;

    private User testUser;
    private Feedback testFeedback;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        testFeedback = new Feedback();
        testFeedback.setId(1L);
        testFeedback.setUser(testUser);
        testFeedback.setRating(5);
        testFeedback.setSubject("Great Service");
        testFeedback.setMessage("Excellent vaccination drive");
        testFeedback.setStatus(FeedbackStatus.PENDING);
        testFeedback.setCreatedAt(LocalDateTime.now());
    }

    // ==================== POSITIVE TESTS ====================

    @Test
    void submitFeedback_Success() {
        FeedbackRequest request = new FeedbackRequest("Great Service", "Excellent vaccination drive", 5);

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        ApiMessage result = feedbackService.submitFeedback(request, testUser);

        assertNotNull(result);
        assertEquals("Feedback submitted successfully", result.message());
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    void submitFeedback_WithRating_Success() {
        FeedbackRequest request = new FeedbackRequest("Test", "Test message", 4);

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        ApiMessage result = feedbackService.submitFeedback(request, testUser);

        assertNotNull(result);
        assertEquals("Feedback submitted successfully", result.message());
    }

    @Test
    void submitFeedback_WithoutRating_Success() {
        FeedbackRequest request = new FeedbackRequest("Test", "Test message", null);

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        ApiMessage result = feedbackService.submitFeedback(request, testUser);

        assertNotNull(result);
    }

    @Test
    void getUserFeedback_Success() {
        when(feedbackRepository.findAll()).thenReturn(List.of(testFeedback));

        List<Map<String, Object>> result = feedbackService.getUserFeedback(1L);

        assertEquals(1, result.size());
        assertEquals("test@example.com", result.get(0).get("userEmail"));
    }

    @Test
    void getAllFeedback_Success() {
        when(feedbackRepository.findAll()).thenReturn(List.of(testFeedback));

        List<Map<String, Object>> result = feedbackService.getAllFeedback();

        assertEquals(1, result.size());
    }

    @Test
    void getFeedbackById_Success() {
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(testFeedback));

        Map<String, Object> result = feedbackService.getFeedbackById(1L);

        assertNotNull(result);
        assertEquals(1L, result.get("id"));
    }

    @Test
    void getFeedbackByIdResponse_Success() {
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(testFeedback));

        FeedbackResponse result = feedbackService.getFeedbackByIdResponse(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("test@example.com", result.getUserEmail());
    }

    @Test
    void getAllFeedback_Paged_Success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Feedback> feedbackPage = new PageImpl<>(List.of(testFeedback));
        when(feedbackRepository.findAll(pageable)).thenReturn(feedbackPage);

        Page<FeedbackResponse> result = feedbackService.getAllFeedback(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void respondToFeedback_Success() {
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(testFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenReturn(testFeedback);

        ApiMessage result = feedbackService.respondToFeedback(1L, "Thank you for your feedback");

        assertNotNull(result);
        assertEquals("Response sent successfully", result.message());
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    void respondToFeedbackWithResponse_Success() {
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(testFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FeedbackResponse result = feedbackService.respondToFeedbackWithResponse(1L, "Thank you!");

        assertNotNull(result);
        assertEquals("Thank you!", result.getAdminResponse());
        assertEquals("REPLIED", result.getStatus());
    }


    @Test
    void getUserIdByEmail_Success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Long result = feedbackService.getUserIdByEmail("test@example.com");

        assertEquals(1L, result);
    }

    // ==================== NEGATIVE TESTS ====================

    @Test
    void submitFeedback_NullSubject_ThrowsException() {
        FeedbackRequest request = new FeedbackRequest(null, "Test message", null);

        assertThrows(IllegalArgumentException.class, () -> feedbackService.submitFeedback(request, testUser));
    }

    @Test
    void submitFeedback_NullMessage_ThrowsException() {
        FeedbackRequest request = new FeedbackRequest("Test", null, null);

        assertThrows(IllegalArgumentException.class, () -> feedbackService.submitFeedback(request, testUser));
    }

    @Test
    void getFeedbackById_NotFound_ThrowsException() {
        when(feedbackRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> feedbackService.getFeedbackById(999L));
    }

    @Test
    void getFeedbackByIdResponse_NotFound_ThrowsException() {
        when(feedbackRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> feedbackService.getFeedbackByIdResponse(999L));
    }

    @Test
    void respondToFeedback_NotFound_ThrowsException() {
        when(feedbackRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> feedbackService.respondToFeedback(999L, "Response"));
    }

    @Test
    void respondToFeedbackWithResponse_NotFound_ThrowsException() {
        when(feedbackRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> feedbackService.respondToFeedbackWithResponse(999L, "Response"));
    }

    @Test
    void getUserIdByEmail_NotFound_ThrowsException() {
        when(userRepository.findByEmail("invalid@example.com")).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> feedbackService.getUserIdByEmail("invalid@example.com"));
    }

    // ==================== BOUNDARY TESTS ====================

    @Test
    void submitFeedback_MaxSubjectLength_Success() {
        FeedbackRequest request = new FeedbackRequest("a".repeat(100), "Test message", null);

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        ApiMessage result = feedbackService.submitFeedback(request, testUser);

        assertNotNull(result);
    }

    @Test
    void submitFeedback_MaxMessageLength_Success() {
        FeedbackRequest request = new FeedbackRequest("Test", "a".repeat(2000), null);

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        ApiMessage result = feedbackService.submitFeedback(request, testUser);

        assertNotNull(result);
    }

    @Test
    void submitFeedback_RatingZero_Success() {
        FeedbackRequest request = new FeedbackRequest("Test", "Test message", 0);

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        ApiMessage result = feedbackService.submitFeedback(request, testUser);

        assertNotNull(result);
    }

    @Test
    void submitFeedback_RatingMax_Success() {
        FeedbackRequest request = new FeedbackRequest("Test", "Test message", 5);

        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> {
            Feedback f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        ApiMessage result = feedbackService.submitFeedback(request, testUser);

        assertNotNull(result);
    }

    // ==================== EDGE CASES ====================

    @Test
    void getUserFeedback_NoFeedback_ReturnsEmptyList() {
        when(feedbackRepository.findAll()).thenReturn(List.of());

        List<Map<String, Object>> result = feedbackService.getUserFeedback(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void getUserFeedback_NullUser_ReturnsEmptyList() {
        Feedback nullUserFeedback = new Feedback();
        nullUserFeedback.setId(2L);
        nullUserFeedback.setUser(null);
        
        when(feedbackRepository.findAll()).thenReturn(List.of(nullUserFeedback));

        List<Map<String, Object>> result = feedbackService.getUserFeedback(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void respondToFeedback_AlreadyResponded_UpdatesResponse() {
        testFeedback.setResponse("Previous response");
        testFeedback.setStatus(FeedbackStatus.REPLIED);
        
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(testFeedback));
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiMessage result = feedbackService.respondToFeedback(1L, "New response");

        assertNotNull(result);
        verify(feedbackRepository).save(any(Feedback.class));
    }

    @Test
    void getFeedbackById_IncludesAllFields() {
        testFeedback.setResponse("Test response");
        testFeedback.setStatus(FeedbackStatus.REPLIED);
        
        when(feedbackRepository.findById(1L)).thenReturn(Optional.of(testFeedback));

        Map<String, Object> result = feedbackService.getFeedbackById(1L);

        assertEquals(1L, result.get("id"));
        assertEquals("test@example.com", result.get("userEmail"));
        assertEquals(5, result.get("rating"));
        assertEquals("Great Service", result.get("subject"));
        assertEquals("Excellent vaccination drive", result.get("message"));
        assertEquals("Test response", result.get("response"));
        assertEquals("REPLIED", (String) result.get("status"));
    }
}

