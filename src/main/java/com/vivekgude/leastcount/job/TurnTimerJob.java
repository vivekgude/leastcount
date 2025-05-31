package com.vivekgude.leastcount.job;

import com.vivekgude.leastcount.redis.GameCache;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TurnTimerJob extends BaseJob {

    private final GameCache gameCache;

    TurnTimerJob(GameCache gameCache) {
        this.gameCache = gameCache;
    }

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        //TODO
    }
}
