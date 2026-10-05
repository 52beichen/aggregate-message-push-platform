package dev.qingzhou.pushserver.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import dev.qingzhou.pushserver.model.vo.portal.DashboardStatsResponse;
import dev.qingzhou.pushserver.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private DashboardService dashboardService;

    @Mock
    private PortalMessageLogService messageLogService;

    @Mock
    private PortalWecomAppService appService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dashboardService = new DashboardServiceImpl(messageLogService, appService);
    }

    @Test
    void testFetchStatsWithNoLogs() {
        Long userId = 1L;
        when(messageLogService.count(any(Wrapper.class))).thenReturn(0L);
        when(appService.count(any(Wrapper.class))).thenReturn(0L);
        when(messageLogService.getOne(any(Wrapper.class), any(Boolean.class))).thenReturn(null);

        DashboardStatsResponse stats = dashboardService.fetchStats(userId);

        assertEquals(0L, stats.getTodayTotal());
        assertEquals(100.0, stats.getSuccessRate(), "Success rate should be 100% when no logs exist");
    }
}
