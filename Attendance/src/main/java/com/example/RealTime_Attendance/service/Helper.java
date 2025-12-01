package com.example.RealTime_Attendance.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Helper {
    public static String durationToTime(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return "0s";
        }

        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append(":");
        if (minutes > 0) sb.append(minutes).append(":");
        if (seconds > 0) sb.append(seconds).append(":");

        return sb.toString().trim();
    }

    public static String localDateTimeToString(LocalDateTime localDateTime){
        if (localDateTime == null) {
            return "No Time Logged";
        }

        LocalTime lt = localDateTime.toLocalTime();
        long hours = lt.getHour();
        long minutes = lt.getMinute() % 60;
        long seconds = lt.getSecond() % 60;


        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append(":");
        else sb.append("00:");
        if (minutes > 0) sb.append(minutes);
        else sb.append("00");

        return sb.toString().trim();

    }

    public static Double DurationToPercentage(Duration duration){
        double diffTime = (double) duration.toMinutes() / 60;
        double rounded = Math.round(diffTime * 10.0) / 10.0;
        return rounded;
    }

}
