package com.vaccine.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerService {
    private final INotificationService notificationService;

    @Scheduled(fixedDelayString = "${app.notifications.dispatch-fixed-delay-ms:60000}", initialDelayString = "${app.notifications.dispatch-initial-delay-ms:15000}")
    public void dispatchNotifications() {
        log.debug("Running notification dispatch cycle");
        notificationService.dispatchDueNotifications();
    }

    @Scheduled(fixedDelayString = "${app.notifications.reconcile-fixed-delay-ms:900000}", initialDelayString = "${app.notifications.reconcile-initial-delay-ms:30000}")
    public void reconcileNotifications() {
        log.debug("Running notification reconcile cycle");
        notificationService.reconcileScheduledNotifications();
    }
}
