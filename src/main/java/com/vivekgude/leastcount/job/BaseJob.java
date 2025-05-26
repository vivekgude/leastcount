package com.vivekgude.leastcount.job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseJob extends QuartzJobBean {
    
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Starting job: {}", getClass().getSimpleName());
            executeJob(context);
            log.info("Completed job: {}", getClass().getSimpleName());
        } catch (Exception e) {
            log.error("Error executing job: {}", getClass().getSimpleName(), e);
            throw new JobExecutionException(e);
        }
    }

    protected abstract void executeJob(JobExecutionContext context) throws Exception;
} 