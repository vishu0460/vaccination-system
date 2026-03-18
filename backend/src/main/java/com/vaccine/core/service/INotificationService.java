package com.vaccine.core.service;

import com.vaccine.domain.Booking;
import com.vaccine.domain.Notification;
import java.util.List;

public interface INotificationService {
    void sendBookingNotification(Booking booking);
    
    void sendEmail(User user, String subject, String message);
    
    void sendSms(User user, String message);
    
    List<Notification> getAllNotifications();
    
    void sendReminderNotification(Booking booking);
}

