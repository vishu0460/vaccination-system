package com.vaccine.core.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlotNotificationService {
    
    private final Map<String, Set<Long>> userSubscriptions = new ConcurrentHashMap<>();
    
    public void subscribe(String userEmail, Long driveId) {
        userSubscriptions.computeIfAbsent(userEmail, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(driveId);
        log.info("User {} subscribed to drive {}", userEmail, driveId);
    }
    
    public void unsubscribe(String userEmail, Long driveId) {
        Set<Long> subs = userSubscriptions.get(userEmail);
        if (subs != null) {
            subs.remove(driveId);
            if (subs.isEmpty()) {
                userSubscriptions.remove(userEmail);
            }
        }
        log.info("User {} unsubscribed from drive {}", userEmail, driveId);
    }
    
    public List<Map<String, Object>> getUserSubscriptions(String userEmail) {
        Set<Long> drives = userSubscriptions.getOrDefault(userEmail, Collections.emptySet());
        return drives.stream()
            .map(driveId -> {
                Map<String, Object> map = new HashMap<>();
                map.put("driveId", driveId);
                map.put("subscribedAt", new Date());
                return map;
            })
            .toList();
    }
}

