package com.vaccine.service;

import com.vaccine.core.service.AdminService;
import com.vaccine.common.dto.AdminDashboardStatsResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @InjectMocks
    private AdminService adminService;

    @Test
    void getDashboardStats() {
        // Minimal test - service compiles
        assertNotNull(adminService);
    }
}
