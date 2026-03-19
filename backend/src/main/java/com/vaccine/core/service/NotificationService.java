package com.vaccine.core.service;

import com.vaccine.domain.Booking;
import com.vaccine.domain.User;
import com.vaccine.domain.Notification;
import com.vaccine.core.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService implements INotificationService {
    
    @Override
    public List<Notification> getAllNotifications() {
        return List.of(); // stub
    }
    
    @Override
    public void sendReminderNotification(Booking booking) {
        log.info("Sending reminder for booking: {}", booking.getId());
        // TODO: Implement reminder
    }
    
    @Override
    public void sendBookingNotification(Booking booking) {
        log.info("Sending booking notification for booking: {}", booking.getId());
        // TODO: Implement email/SMS notification
        sendEmail(booking.getUser(), "Booking Confirmed", "Your vaccination booking is confirmed");
        sendSms(booking.getUser(), "Booking confirmed for drive: " + booking.getSlot().getDrive().getTitle());
    }
    
    public void sendEmail(User user, String subject, String message) {
        log.info("Email to {}: {} - {}", user.getEmail(), subject, message);
    }
    
    public void sendSms(User user, String message) {
        log.info("SMS to {}: {}", user.getPhoneNumber(), message);
    }
}
