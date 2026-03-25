package com.vaccine.core.service;

import com.vaccine.common.exception.AppException;
import com.vaccine.domain.DriveSubscription;
import com.vaccine.domain.User;
import com.vaccine.domain.VaccinationDrive;
import com.vaccine.infrastructure.persistence.repository.DriveSubscriptionRepository;
import com.vaccine.infrastructure.persistence.repository.UserRepository;
import com.vaccine.infrastructure.persistence.repository.VaccinationDriveRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SlotNotificationService {

    private final DriveSubscriptionRepository driveSubscriptionRepository;
    private final UserRepository userRepository;
    private final VaccinationDriveRepository driveRepository;

    public void subscribe(String userEmail, Long driveId) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new AppException("User not found"));
        VaccinationDrive drive = driveRepository.findById(driveId)
            .orElseThrow(() -> new AppException("Drive not found"));

        if (!driveSubscriptionRepository.existsByUserEmailAndDriveId(userEmail, driveId)) {
            driveSubscriptionRepository.save(DriveSubscription.builder()
                .user(user)
                .drive(drive)
                .build());
        }

        log.info("User {} subscribed to drive {}", userEmail, driveId);
    }

    public void unsubscribe(String userEmail, Long driveId) {
        driveRepository.findById(driveId)
            .orElseThrow(() -> new AppException("Drive not found"));
        driveSubscriptionRepository.deleteByUserEmailAndDriveId(userEmail, driveId);
        log.info("User {} unsubscribed from drive {}", userEmail, driveId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserSubscriptions(String userEmail) {
        return driveSubscriptionRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
            .map(subscription -> {
                Map<String, Object> map = new HashMap<>();
                map.put("driveId", subscription.getDrive().getId());
                map.put("subscribedAt", subscription.getCreatedAt());
                return map;
            })
            .toList();
    }
}
