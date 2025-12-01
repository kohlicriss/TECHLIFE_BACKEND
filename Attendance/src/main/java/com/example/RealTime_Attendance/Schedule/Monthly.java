package com.example.RealTime_Attendance.Schedule;

import com.example.RealTime_Attendance.Schedule.Jobs.GeneratePaySlipsAndLeavesInit;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.JobBuilder.newJob;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Monthly {

    @Autowired
    private Scheduler scheduler;

    @PostConstruct
    public void init() throws SchedulerException {
        schedulePaySlipsAndLeavesInit();
        scheduler.start();
    }

    public void schedulePaySlipsAndLeavesInit() throws SchedulerException {
        JobDetail job = newJob(GeneratePaySlipsAndLeavesInit.class)
                .withIdentity("Generate_paySlips_and_leavesInit_1","Month_group" )
                .withDescription("Generates Payslips then initialise all other details like paid, sick leaves and make effective hours to zero etc...")
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("Generate_paySlips_and_leavesInit_trigger_1", "monthly_trigger_group")
                .forJob(job)
                .withSchedule(cronSchedule("0 0 0 1 * ?"))
                .build();
        if (!scheduler.checkExists(job.getKey())) {
            scheduler.scheduleJob(job, trigger);
            log.info("Monthly job scheduled successfully for every month on 1st day.");
        } else {
            log.info("Monthly job already exists in scheduler. Skipping creation.");
        }
    }
}

