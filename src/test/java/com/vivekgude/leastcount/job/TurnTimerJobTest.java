package com.vivekgude.leastcount.job;

import com.vivekgude.leastcount.enums.GameState;
import com.vivekgude.leastcount.redis.GameCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TurnTimerJobTest {

    @Mock
    private GameCache gameCache;
    @Mock
    private JobSchedulerService jobSchedulerService;
    @Mock
    private JobExecutionContext context;
    @Mock
    private JobDetail jobDetail;

    private TurnTimerJob job;

    @BeforeEach
    void setUp() {
        job = new TurnTimerJob(gameCache, jobSchedulerService);
    }

    @Test
    void onExpiryAdvancesTurnAndReschedules() throws Exception {
        String gameId = "G1";
        long current = 101L;
        long next = 102L;

        when(context.getJobDetail()).thenReturn(jobDetail);
        JobDataMap map = new JobDataMap();
        map.put("gameId", gameId);
        when(jobDetail.getJobDataMap()).thenReturn(map);

        when(gameCache.getGameState(gameId)).thenReturn(GameState.INPROGRESS.getType());
        when(gameCache.getMoveTime(gameId)).thenReturn(System.currentTimeMillis() - 1000);
        when(gameCache.getCurrentPlayer(gameId)).thenReturn(current);
        when(gameCache.getActivePlayers(gameId)).thenReturn(List.of(current, next));
        when(gameCache.getMoveTimeConfigOrNull(gameId)).thenReturn(1000L);

        job.executeJob(context);

        verify(gameCache, times(1)).addFieldToMap(eq("game:" + gameId), eq("currentPlayer"), eq(String.valueOf(next)));
        verify(jobSchedulerService, times(1)).deleteJob("turnTimer_" + gameId);
        verify(jobSchedulerService, times(1)).scheduleOneTimeJob(eq("turnTimer_" + gameId), eq(TurnTimerJob.class), any(), anyMap());
    }
}


