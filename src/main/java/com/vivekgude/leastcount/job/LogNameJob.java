package com.vivekgude.leastcount.job;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogNameJob extends BaseJob {

    @Override
    protected void executeJob(JobExecutionContext context) throws Exception {
        // example job
        log.info(context.toString());
    }
} 