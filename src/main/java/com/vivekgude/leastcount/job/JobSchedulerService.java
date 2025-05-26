package com.vivekgude.leastcount.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobSchedulerService {

    private final Scheduler scheduler;

    public void scheduleJob(String jobName, Class<? extends BaseJob> jobClass, String cronExpression) throws SchedulerException {
        scheduleJob(jobName, jobClass, cronExpression, null);
    }

    public void scheduleJob(String jobName, Class<? extends BaseJob> jobClass, String cronExpression, Map<String, Object> jobData) throws SchedulerException {
        JobDetail jobDetail = buildJobDetail(jobName, jobClass, jobData);
        Trigger trigger = buildCronTrigger(jobName, cronExpression);
        
        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Job {} scheduled successfully", jobName);
    }

    public void scheduleOneTimeJob(String jobName, Class<? extends BaseJob> jobClass, Date startTime) throws SchedulerException {
        scheduleOneTimeJob(jobName, jobClass, startTime, null);
    }

    public void scheduleOneTimeJob(String jobName, Class<? extends BaseJob> jobClass, Date startTime, Map<String, Object> jobData) throws SchedulerException {
        JobDetail jobDetail = buildJobDetail(jobName, jobClass, jobData);
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(jobName + "_trigger")
                .startAt(startTime)
                .build();
        
        scheduler.scheduleJob(jobDetail, trigger);
        log.info("One-time job {} scheduled for {}", jobName, startTime);
    }

    public void pauseJob(String jobName) throws SchedulerException {
        scheduler.pauseJob(JobKey.jobKey(jobName));
        log.info("Job {} paused", jobName);
    }

    public void resumeJob(String jobName) throws SchedulerException {
        scheduler.resumeJob(JobKey.jobKey(jobName));
        log.info("Job {} resumed", jobName);
    }

    public void deleteJob(String jobName) throws SchedulerException {
        scheduler.deleteJob(JobKey.jobKey(jobName));
        log.info("Job {} deleted", jobName);
    }

    private JobDetail buildJobDetail(String jobName, Class<? extends BaseJob> jobClass, Map<String, Object> jobData) {
        JobBuilder jobDetailBuilder = JobBuilder.newJob(jobClass)
                .withIdentity(jobName)
                .storeDurably();

        if (jobData != null) {
            jobDetailBuilder.usingJobData(new JobDataMap(jobData));
        }

        return jobDetailBuilder.build();
    }

    private Trigger buildCronTrigger(String jobName, String cronExpression) {
        return TriggerBuilder.newTrigger()
                .withIdentity(jobName + "_trigger")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }
} 