package com.vaccine.core.service;

import com.vaccine.common.exception.AppException;
import com.vaccine.domain.DriveSubscription;
import com.vaccine.domain.User;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.infrastructure.persistence.repository.DriveSubscriptionRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlotNotificationServiceTest {

    @Mock
    private DriveSubscriptionRepository driveSubscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VaccinationDriveRepository driveRepository;

    @InjectMocks
    private SlotNotificationService slotNotificationService;

    private User user;
    private VaccinationDrive drive;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .id(1L)
            .email("user@test.com")
            .build();

        drive = VaccinationDrive.builder()
            .id(11L)
            .title("Community Drive")
            .build();
    }

    @Test
    void subscribe_PersistsSubscriptionWhenItDoesNotExist() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(driveRepository.findById(11L)).thenReturn(Optional.of(drive));
        when(driveSubscriptionRepository.existsByUserEmailAndDriveId("user@test.com", 11L)).thenReturn(false);

        slotNotificationService.subscribe("user@test.com", 11L);

        verify(driveSubscriptionRepository).save(ArgumentMatchers.any(DriveSubscription.class));
    }

    @Test
    void subscribe_DoesNotDuplicateExistingSubscription() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(driveRepository.findById(11L)).thenReturn(Optional.of(drive));
        when(driveSubscriptionRepository.existsByUserEmailAndDriveId("user@test.com", 11L)).thenReturn(true);

        slotNotificationService.subscribe("user@test.com", 11L);

        verify(driveSubscriptionRepository, never()).save(ArgumentMatchers.any(DriveSubscription.class));
    }

    @Test
    void subscribe_RejectsUnknownDrive() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(driveRepository.findById(11L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> slotNotificationService.subscribe("user@test.com", 11L));
    }

    @Test
    void getUserSubscriptions_ReturnsPersistedCreatedAtValue() {
        LocalDateTime subscribedAt = LocalDateTime.now();
        DriveSubscription subscription = DriveSubscription.builder()
            .id(100L)
            .user(user)
            .drive(drive)
            .createdAt(subscribedAt)
            .build();

        when(driveSubscriptionRepository.findByUserEmailOrderByCreatedAtDesc("user@test.com"))
            .thenReturn(List.of(subscription));

        List<Map<String, Object>> result = slotNotificationService.getUserSubscriptions("user@test.com");

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).get("driveId"));
        assertEquals(subscribedAt, result.get(0).get("subscribedAt"));
    }
}
