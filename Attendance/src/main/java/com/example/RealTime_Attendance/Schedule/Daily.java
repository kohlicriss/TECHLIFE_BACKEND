package com.example.RealTime_Attendance.Schedule;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;

import com.example.RealTime_Attendance.Dto.ScheduleAttendance;
import com.example.RealTime_Attendance.Exception.CustomException;
import com.example.RealTime_Attendance.Schedule.Jobs.AttendanceCheck;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.ZoneId;
import java.util.TimeZone;
import java.util.*;

@Service
@Slf4j
public class Daily {

    @Autowired
    private Scheduler scheduler;

    public void scheduleAttendanceJob(ScheduleAttendance scheduleAttendance) throws SchedulerException, ParseException {
        String shiftName = scheduleAttendance.getShift();
        String section = String.valueOf(scheduleAttendance.getSection());
        String cronString = scheduleAttendance.getCronExpression();
        CronExpression cronExpression = new CronExpression(cronString);
        String zone = scheduleAttendance.getZone();

        JobDetail job = newJob(AttendanceCheck.class)
                .withIdentity("attendance_check_" + shiftName + "_" + section, shiftName)
                .usingJobData("shiftName", shiftName)
                .usingJobData("section", section)
                .usingJobData("zone", zone)
                .withDescription("Checks employee presence for shift: " + shiftName)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("attendance_trigger_" + shiftName + "_" + section, shiftName)
                .withSchedule(CronScheduleBuilder
                        .cronSchedule(cronExpression)
                        .inTimeZone(TimeZone.getTimeZone(ZoneId.of(zone))))
                .forJob(job)
                .build();

        if (scheduler.checkExists(job.getKey())) {
            throw new CustomException(
                    String.format("Job already scheduled with key %s", job.getKey()), HttpStatus.ALREADY_REPORTED);
        }

        scheduler.scheduleJob(job, trigger);
        log.info("✅ Attendance check scheduled for {} with cron: {}", shiftName, cronString);
    }

    public void updateAttendanceJob(ScheduleAttendance scheduleAttendance) throws SchedulerException, ParseException {
        String shiftName = scheduleAttendance.getShift();
        String section = String.valueOf(scheduleAttendance.getSection());
        String cronString = scheduleAttendance.getCronExpression();
        String zone = scheduleAttendance.getZone();

        JobKey jobKey = new JobKey("attendance_check_" + shiftName + "_" + section, shiftName);
        TriggerKey triggerKey = new TriggerKey("attendance_trigger_" + shiftName + "_" + section, shiftName);

        if (!scheduler.checkExists(jobKey)) {
            throw new CustomException("Job not found for update", HttpStatus.NOT_FOUND);
        }

        CronExpression cronExpression = new CronExpression(cronString);

        Trigger newTrigger = TriggerBuilder.newTrigger()
                .withIdentity(triggerKey)
                .withSchedule(CronScheduleBuilder
                        .cronSchedule(cronExpression)
                        .inTimeZone(TimeZone.getTimeZone(ZoneId.of(zone))))
                .build();

        scheduler.rescheduleJob(triggerKey, newTrigger);
        log.info("♻️ Job updated successfully for {}_{}", shiftName, section);
    }

    public void deleteJob(String shiftName, String section) throws SchedulerException {
        JobKey jobKey = new JobKey("attendance_check_" + shiftName + "_" + section, shiftName);
        if (!scheduler.checkExists(jobKey)) {
            throw new CustomException("Job not found", HttpStatus.NOT_FOUND);
        }

        scheduler.deleteJob(jobKey);
        log.info("Job deleted for {}_{}", shiftName, section);
    }

    public List<Map<String, Object>> getJobByIdentity(String groupName) throws SchedulerException {
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
        List<Map<String, Object>> result = new ArrayList<>();

        for (JobKey jobKey : jobKeys) {
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            Map<String, Object> jobInfo = new HashMap<>();
            jobInfo.put("name", jobKey.getName());
            jobInfo.put("group", jobKey.getGroup());
            jobInfo.put("description", jobDetail.getDescription());
            jobInfo.put("class", jobDetail.getJobClass().getName());

            // Optional: cron trigger info
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            if (!triggers.isEmpty() && triggers.get(0) instanceof CronTrigger) {
                CronTrigger cronTrigger = (CronTrigger) triggers.get(0);
                jobInfo.put("cronExpression", cronTrigger.getCronExpression());
                jobInfo.put("nextFireTime", cronTrigger.getNextFireTime());
                jobInfo.put("previousFireTime", cronTrigger.getPreviousFireTime());
            }

            result.add(jobInfo);
        }

        return result;
    }

    public List<String> getJobsByGroup(String groupName) throws SchedulerException {
        Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName));
        List<String> jobList = new ArrayList<>();
        for (JobKey key : jobKeys) {
            jobList.add(key.getName());
        }
        return jobList;
    }

    public List<Map<String, String>> getAllJobs() throws SchedulerException {
        List<String> groupNames = scheduler.getJobGroupNames();
        List<Map<String, String>> jobs = new ArrayList<>();

        for (String group : groupNames) {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group));
            for (JobKey key : jobKeys) {
                Map<String, String> jobMap = new HashMap<>();
                jobMap.put("group", group);
                jobMap.put("jobName", key.getName());
                jobs.add(jobMap);
            }
        }
        return jobs;
    }
}