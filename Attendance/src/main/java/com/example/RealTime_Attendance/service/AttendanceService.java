package com.example.RealTime_Attendance.service;

import com.example.RealTime_Attendance.Dto.Clock;
import com.example.RealTime_Attendance.Exception.CustomException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.example.RealTime_Attendance.Dto.*;
import com.example.RealTime_Attendance.Entity.Attendance;
import com.example.RealTime_Attendance.Entity.Break;
import com.example.RealTime_Attendance.Entity.LeaveEntity;
import com.example.RealTime_Attendance.Entity.PersonalLeaves;
import com.example.RealTime_Attendance.Entity.Shifts;
import com.example.RealTime_Attendance.Enums.WorkingDaysType;
import com.example.RealTime_Attendance.repository.AttendanceRepository;
import com.example.RealTime_Attendance.repository.BreaksRepository;
import com.example.RealTime_Attendance.repository.LeaveEntityRepository;
import com.example.RealTime_Attendance.repository.PersonalLeavesRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private StringRedisTemplate redisTemplate;
    private final BreaksRepository breaksRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveEntityRepository leaveRepo;

    private final PersonalLeavesRepository personalLeavesRepository;

    @Transactional
    public Attendance addAttendance(Attendance attendance) {
        List<Break> temp = attendance.getBreaks();
        attendance.setBreaks(new ArrayList<>());
        Attendance saved = attendanceRepository.save(attendance);
        for (Break b : temp) {
            b.setAttendance(saved);
            breaksRepository.save(b);
        }
        return attendanceRepository.getReferenceById(saved.getId());
    }

    public String disconnected(String attendanceId, Clock clock) {
        LocalTime time = LocalTime.now(ZoneId.of(clock.getTimeZone()));
        LocalDate today = LocalDate.now(ZoneId.of(clock.getTimeZone()));
        LocalDateTime now = LocalDateTime.now(ZoneId.of(clock.getTimeZone()));
        String mode = clock.getMode();
        Optional<Attendance> attendance = attendanceRepository.findByAttendanceId(attendanceId);
        if (attendance.isEmpty())
            return "Attendance not found";
        if (attendance.get().getLoginTime() == null)
            return "user not logged in";
        if (attendance.get().getLogoutTime() != null)
            return "Already Disconnected";
        Break newBreak = new Break();
        newBreak.setStartTime(now);
        newBreak.setAttendance(attendance.get());

        attendance.get().getBreaks().add(newBreak);
        attendanceRepository.save(attendance.get());
        log.info("Employee {} Disconnected and Break Started at {} for Attendance {}", attendance.get().getEmployeeId(),
                newBreak.getStartTime(), attendance.get().getAttendanceId());

        return "DISCONNECTED - Break Started";
    }

    public String connected(String attendanceId, Clock clock) {
        LocalDate today = LocalDate.now(ZoneId.of(clock.getTimeZone()));
        LocalDateTime now = LocalDateTime.now(ZoneId.of(clock.getTimeZone()));
        String mode = clock.getMode();
        Optional<Attendance> exists = attendanceRepository.findByAttendanceId(attendanceId);
        if (exists.isEmpty())
            throw new CustomException("Attendance not found", HttpStatus.NOT_FOUND);
        Attendance attendance = exists.get();
        if (!attendance.isAttended())
            throw new CustomException("Attendance already Marked as absent", HttpStatus.BAD_REQUEST);
        if (attendance.getLogoutTime() != null)
            throw new CustomException("Already Disconnected", HttpStatus.BAD_REQUEST);
        if (attendance.getBreaks().isEmpty())
            throw new CustomException("No ongoing break to end", HttpStatus.BAD_REQUEST);

        List<Break> breaks = attendance.getBreaks();

        Break ongoingBreak = breaks.stream()
                .filter(b -> b.getEndTime() == null)
                .reduce((first, second) -> second)
                .orElse(null);

        if (ongoingBreak != null) {
            ongoingBreak.setEndTime(now);
            attendanceRepository.save(attendance);
            log.info("Employee {} Connected and Break Ended at {} for Attendance {}", attendance.getEmployeeId(),
                    ongoingBreak.getEndTime(), attendance.getAttendanceId());
            return "CONNECTED - Break Ended";
        } else {
            return "No ongoing break to end";
        }
    }

    @Transactional
    @Async("attendanceExecutor")
    public CompletableFuture<SimpleAttendanceDto> clockIn(String employeeId, Clock clock) {
        LocalTime time = LocalTime.now(ZoneId.of(clock.getTimeZone()));
        LocalDate today = LocalDate.now(ZoneId.of(clock.getTimeZone()));
        LocalDateTime now = LocalDateTime.now(ZoneId.of(clock.getTimeZone()));
        String mode = clock.getMode();
        System.out.println(employeeId+"_"+today);
        Attendance attendance = attendanceRepository.findByAttendanceId(employeeId + "_" + today).orElse(new Attendance());

        if (attendance.getLoginTime() == null ) {
            log.info("As the employee {} is not clocked in, for mode {} so now clocking in", employeeId, mode);
            if (mode == null)
                mode = "Office";

            Optional<PersonalLeaves> personalLeaves = personalLeavesRepository
                    .findByEmployeeIdAndMonthAndYear(employeeId, (Integer) today.getMonthValue(), (Integer) today.getYear());
            if (personalLeaves.isEmpty()) throw new CustomException("Personal Leaves for this employees Does not exists, make sure first fill the attendance form",  HttpStatus.NOT_FOUND);
            Shifts shift = personalLeaves.get().getShifts();
            LocalTime shifTime = shift.getStartTime();
            LocalTime HalfTime = shift.getHalfTime();

            if (shifTime.isAfter(time)) {
                log.info("Employee {} is on time", employeeId);
                Long ot = personalLeaves.get().getMonthlyOnTime() == null ? Long.valueOf(0) : personalLeaves.get().getMonthlyOnTime();
                personalLeaves.get().setMonthlyOnTime(ot + 1);
                personalLeavesRepository.save(personalLeaves.get());
            } else {
                log.info("Employee {} is late as your shift time starts at {}", employeeId, shifTime);
            }

            attendance.setEmployeeId(employeeId);
            attendance.setDate(today);
            attendance.setMonth(today.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH));
            attendance.setYear(today.getYear());
            attendance.setLoginTime(now);
            attendance.setAttended(true);
            attendance.setMode(mode);
            attendance.setAttendanceId(employeeId + "_" + today);
            attendance.setDevice(clock.getDevice());
            attendance.setTimeZone(clock.getTimeZone());
            attendance.setLatitude(clock.getLatitude());
            attendance.setLongitude(clock.getLongitude());
            Attendance saved = attendanceRepository.save(attendance);
            log.info("Employee {} clocked in at {} Created Attendance {}", employeeId, saved.getLoginTime(),
                    saved.getAttendanceId());
            evictCache(employeeId);
            return CompletableFuture.completedFuture(saved.toSimpleAttendanceDto());
        }
        // if (!attendance.get().isAttended()) {
        //     log.info("As the employee {} user is marked as absent, for mode {}", employeeId, mode);
        //     throw new CustomException("Employee Already Marked as Absent", HttpStatus.BAD_REQUEST);
        // }

        if (attendance.getLogoutTime() != null) {
            LocalDateTime LogOuttime = attendance.getLogoutTime();
            attendance.setLogoutTime(null);
            Break newBreak = new Break();
            newBreak.setStartTime(LogOuttime);
            newBreak.setEndTime(now);
            newBreak.setAttendance(attendance);
            attendance.getBreaks().add(newBreak);
            log.info(
                    "As the user is already clocked in at {}, for mode {} considering last logout time {} to now as break ",
                    attendance.getLoginTime(), mode, LogOuttime);
            return CompletableFuture
                    .completedFuture(attendanceRepository.save(attendance).toSimpleAttendanceDto());
        }
        return CompletableFuture.completedFuture(null);
    }

    private void evictCache(String employeeId) {
        // Set<String> keys = redisTemplate.keys("attendance::" + employeeId + "*");
        // if (keys != null && !keys.isEmpty()) {
        // redisTemplate.delete(keys);
        // }
    }

    @Async("attendanceExecutor")
    @Transactional
    public CompletableFuture<SimpleAttendanceDto> clockOut(String employeeId, Clock clock) {
        LocalTime time = LocalTime.now(ZoneId.of(clock.getTimeZone()));
        LocalDate today = LocalDate.now(ZoneId.of(clock.getTimeZone()));
        LocalDateTime now = LocalDateTime.now(ZoneId.of(clock.getTimeZone()));
        String mode = clock.getMode();
        Attendance attendance = attendanceRepository.findByAttendanceId(employeeId + "_" + today).orElse(new Attendance());

        if (attendance.getLoginTime()==null) {
            log.info("The employee {} is not clocked in", employeeId);
            throw new CustomException("The employee is not Clocked in", HttpStatus.BAD_REQUEST);
        }


//        if (!attendance.isAttended()) {
//            log.info("The employee {} is marked as absent", employeeId);
//            throw new CustomException("Employee Already Marked as Absent", HttpStatus.BAD_REQUEST);
//        }

        if (attendance.getLogoutTime() != null) {
            log.info("The employee {} is already clocked out at {}", employeeId, attendance.getLogoutTime());
            throw new CustomException("Employee Already Clocked Out", HttpStatus.BAD_REQUEST);
        }


        Optional<PersonalLeaves> personalLeavesOpt =
                personalLeavesRepository.findByEmployeeIdAndMonthAndYear(employeeId,
                        today.getMonthValue(), today.getYear());

        if (personalLeavesOpt.isEmpty()) {
            throw new CustomException("Shift details not found for employee " + employeeId, HttpStatus.NOT_FOUND);
        }

        PersonalLeaves personalLeaves = personalLeavesOpt.get();
        Shifts shift = personalLeaves.getShifts();

        LocalTime startTime = shift.getStartTime();
        LocalTime endTime = shift.getEndTime();
        Duration acceptedBreak = shift.getAcceptedBreakTime();

        Duration effectiveTime = getEffectiveHours(
                attendance.getBreaks(),
                attendance.getLoginTime(),
                now);

        Duration actualTime = Duration.between(startTime, endTime).minus(acceptedBreak);
        Duration diff = actualTime.minus(effectiveTime);

        if (diff.isNegative()) diff = Duration.ZERO; // safeguard

        boolean firstHalfPresent = attendance.isFirstSection();
        boolean secondHalfPresent = attendance.isSecondSection();

        Duration oneHour = Duration.ofHours(1);
        Duration twoHours = Duration.ofHours(2);
        Duration halfActualTime = actualTime.dividedBy(2);

        // Case 1: Employee worked overtime
        if (effectiveTime.compareTo(actualTime) > 0) {
            personalLeaves.setMonthlyOvertime(effectiveTime.minus(actualTime));
            personalLeavesRepository.save(personalLeaves);
            attendance.setConsiderPresent(true);
            attendance.setFirstSection(true);
            attendance.setSecondSection(true);
        }
        // Case 2: Worked less than required
        else {
            if (diff.compareTo(oneHour) <= 0) {
                // Negligible shortfall
                attendance.setConsiderPresent(true);
                attendance.setFirstSection(true);
                attendance.setSecondSection(true);
            } else {
                attendance.setConsiderPresent(false);
                // Significant shortfall
                if (!firstHalfPresent) {
                    // FIRST half absent already
                    if (effectiveTime.compareTo(halfActualTime) >= 0) {
                        attendance.setSecondSection(true);
                        
                    } else {
                        attendance.setFirstSection(false);
                        attendance.setSecondSection(false);
                        
                    }
                } else if (!secondHalfPresent) {
                    // Second half absent already
                    if (effectiveTime.compareTo(halfActualTime) >= 0) {
                        attendance.setFirstSection(true);
                    } else {
                        attendance.setFirstSection(false);
                        attendance.setSecondSection(false);
                    }
                } else {
                    // Both halves initially present
                    if (diff.compareTo(twoHours) > 0) {
                        if (effectiveTime.compareTo(halfActualTime) < 0) {
                            attendance.setFirstSection(false);
                            attendance.setSecondSection(false);
                        } else {
                            // Early logout case
                            attendance.setSecondSection(false);
                        }
                    } else {
                        // Slight shortfall (< 2h)
                        attendance.setFirstSection(true);
                        attendance.setSecondSection(false);
                    }
                }
            }
        }

        // Logging and final updates
        log.info("Employee {} clocked out at {} for Attendance {}", employeeId, now,
                attendance.getAttendanceId());
        log.info("Effective hours for the date {} is {}", attendance.getAttendanceId(), effectiveTime);

        attendance.setEffectiveHours(effectiveTime);
        attendance.setLogoutTime(now);
        attendance.setLatitude(clock.getLatitude());
        attendance.setLongitude(clock.getLongitude());
        attendance.setTimeZone(clock.getTimeZone());


        evictCacheing(employeeId);
        return CompletableFuture.completedFuture(attendanceRepository.save(attendance).toSimpleAttendanceDto());
    }

    private void evictCacheing(String employeeId) {
        // Set<String> keys = redisTemplate.keys("attendance::" + employeeId + "*");
        // if (keys != null && !keys.isEmpty()) {
        // redisTemplate.delete(keys);
        // }
    }

    public CompletableFuture<SimpleAttendanceDto> getEmployeeAttendanceDay(String employeeId, int day, int month,
            int year) {
        Optional<Attendance> result = attendanceRepository.findByEmployeeIdAndDate(employeeId,
                LocalDate.of(year, month, day));
        if (result.isEmpty()) {
            log.error("Attendance not fount for employee {} on date {}", employeeId, LocalDate.of(year, month, day));
            String s = String.format("Attendance not fount for employee %s on date %s", employeeId, LocalDate.of(year, month, day));
            throw new CustomException(s, HttpStatus.NOT_FOUND);
        }
        return CompletableFuture.completedFuture(result.get().toSimpleAttendanceDto());
    }

    @Async("attendanceExecutor")
    // @Cacheable(value = "attendance", key = "#employeeId + ':' + #page + ':' +
    // #size")
    public CompletableFuture<List<AttendanceDTO>> getAllAttendanceDTO(String employeeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        System.out.println("fetching from data base");

        Page<Attendance> attendancePage = attendanceRepository.findByEmployeeId(employeeId, pageable);

        List<AttendanceDTO> result = attendancePage.map(attendance -> AttendanceDTO.builder()
                .employee_id(attendance.getEmployeeId())
                .date(attendance.getDate() != null ? attendance.getDate().toString() : null)
                .login_time(Helper.localDateTimeToString(attendance.getLoginTime()))
                .logout_time(Helper.localDateTimeToString(attendance.getLogoutTime()))
                .build()).getContent();

        return CompletableFuture.completedFuture(result);
    }

    public List<PieChartDTO> pieChart(String empId) {
        List<Attendance> attendanceList = attendanceRepository.getByEmployeeId(empId);
        attendanceList.sort(Comparator.comparing(Attendance::getDate).reversed());

        if (attendanceList.size() > 5) {
            attendanceList = attendanceList.subList(0, 5);
        }

        List<PieChartDTO> result = attendanceList.stream()
                .map(Attendance::toPieChartDto)
                .collect(Collectors.toList());

        return (result);
    }

    public List<BarGraphDTO> bargraph(String emp_id, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());
        List<BarGraphDTO> result = attendanceRepository.findByEmployeeId(emp_id, pageable).stream()
                .map(Attendance::toBarGraphDTO)
                .collect(Collectors.toList());
        log.info("Detailed Attendance Summery of Past {} of page {} for employee {}", size, page, emp_id);
        return (result);
    }

    public ProfileTime getProfile(String employeeId, int year, int month, int day) {
        try {
            LocalDate date = LocalDate.of(year, month, day);

            ProfileTime profileTime = new ProfileTime();

            Optional<Attendance> att = attendanceRepository.findByEmployeeIdAndDate(employeeId, date);
            Optional<PersonalLeaves> pl = personalLeavesRepository.findByEmployeeIdAndMonthAndYear(employeeId,
                    (Integer) month, (Integer) year);

            if (att != null && pl != null) {
                Attendance attendance = att.get();
                PersonalLeaves personalLeaves = pl.get();
                profileTime.setMode(attendance.getMode());
                profileTime.setLoginTime(attendance.getLoginTime());
                profileTime.setShift(personalLeaves.getShifts().getShiftName());
                profileTime.setOnTime(personalLeaves.getMonthlyOnTime());
                profileTime.setAvgWorkingHours(personalLeaves.getWeekEffectiveHours());
                profileTime.setGrossTimeDay(attendance.calcGrossHours());
                profileTime.setEffectiveTimeDay(attendance.calcEffectiveHours());
            }
            return profileTime;
        } catch (Exception e) {
            e.printStackTrace();
            return new ProfileTime();
        }
    }

    public List<LinegraphAttedance> lineGraphAttendance(String EmpId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        List<Attendance> attendanceList = attendanceRepository.findByEmployeeId(EmpId, pageable).getContent();
        // attendanceList.sort(Comparator.comparing(Attendance::getDate));

        List<LinegraphAttedance> result = attendanceList.stream()
                .map(Attendance::toLineChartDto)
                .collect(Collectors.toList());
        log.info("Getting last {} days attendance summery for employee {} includes working hours and break hours", size,
                EmpId);
        return (result);
    }

    public List<LeavesBarGraphDTO> leavesGraph(String employeeId) {
        Map<String, Long> dayCounts = leaveRepo.findByEmployeeIdOrderByCreatedAtDesc(employeeId).stream()
                .map(LeaveEntity::toLeavesBarGraphDto)
                .collect(Collectors.groupingBy(
                        LeavesBarGraphDTO::getDay,
                        Collectors.counting()));

        List<String> daysOrder = Arrays.asList("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

        List<LeavesBarGraphDTO> result = daysOrder.stream()
                .map(day -> new LeavesBarGraphDTO(day, dayCounts.getOrDefault(day, 0L).intValue()))
                .collect(Collectors.toList());

        return result;
    }

    @Async("attendanceExecutor")
    public CompletableFuture<List<DashboardMetric>> getDashboardMetrics(String empId, String timeZone) {
        LocalDate today = LocalDate.now(ZoneId.of(timeZone));
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(DayOfWeek.SUNDAY);
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        List<Attendance> records = attendanceRepository.getByEmployeeId(empId);

        double todayWorked = 0, todayExpected = 10;
        double weekWorked = 0, weekExpected = 0;
        double monthWorked = 0, monthExpected = 0;

        for (Attendance r : records) {
            if (r.getLoginTime() != null && r.getLogoutTime() != null) {
                Duration worked = Duration.between(r.getLoginTime(), r.getLogoutTime());

                if (r.getBreaks() != null) {
                    for (Break b : r.getBreaks()) {
                        if (b.getStartTime() != null && b.getEndTime() != null) {
                            worked = worked.minus(Duration.between(b.getStartTime(), b.getEndTime()));
                        }
                    }
                }

                LocalDate date = r.getDate();
                if (date.equals(today))
                    todayWorked += worked.toMinutes() / 60.0;
                if (!date.isBefore(startOfWeek) && !date.isAfter(endOfWeek)) {
                    weekWorked += worked.toMinutes() / 60.0;
                    weekExpected += 9;
                }
                if (!date.isBefore(startOfMonth) && !date.isAfter(endOfMonth)) {
                    monthWorked += worked.toMinutes() / 60.0;
                    monthExpected += 9;
                }
            }
        }

        String todayVal = String.format("%.1f/%.0f", todayWorked, todayExpected);
        String weekVal = String.format("%.1f/%.0f", weekWorked, weekExpected);
        String monthVal = String.format("%.0f/%.0f", monthWorked, monthExpected);

        double overtimeHours = monthWorked > monthExpected ? (monthWorked - monthExpected) : 0;
        String overtimeVal = String.format("%.0f/%.0f", overtimeHours, 28.0);

        List<DashboardMetric> metrics = new ArrayList<>();

        metrics.add(DashboardMetric.builder()
                .value(todayVal)
                .description("Total Hours Today")
                .trend("up")
                .trendPercentage("5")
                .trendPeriod("This week")
                .build());
        metrics.add(DashboardMetric.builder()
                .value(weekVal)
                .description("Total Hours Week")
                .trend("up")
                .trendPercentage("7")
                .trendPeriod("Last week")
                .build());
        metrics.add(DashboardMetric.builder()
                .value(monthVal)
                .description("Total Hours Month")
                .trend("down")
                .trendPercentage("8")
                .trendPeriod("Last Month")
                .build());
        metrics.add(DashboardMetric.builder()
                .value(overtimeVal)
                .description("Overtime this Month")
                .trend("down")
                .trendPercentage("6")
                .trendPeriod("Last Month")
                .build());

        return CompletableFuture.completedFuture(metrics);
    }

    private Duration getEffectiveHours(List<Break> breaks, LocalDateTime loginTime, LocalDateTime logoutTime) {
        Duration totalWorked = Duration.between(loginTime, logoutTime);

        if (breaks != null) {
            for (Break b : breaks) {
                if (b.getStartTime() != null && b.getEndTime() != null) {
                    totalWorked = totalWorked.minus(Duration.between(b.getStartTime(), b.getEndTime()));
                }
            }
        }

        return totalWorked.isNegative() ? Duration.ZERO : totalWorked;
    }

    public boolean isPresent(String employeeId, int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);
        Optional<Attendance> attendance = attendanceRepository.findByEmployeeIdAndDate(employeeId, date);
        return attendance.isPresent() && attendance.get().isAttended();
    }

    public List<AttendanceDTO> getPresentEmployees(int year, int month, int day) {
        LocalDate date = LocalDate.of(year, month, day);
        List<Attendance> attendances = attendanceRepository.findByDateAndIsAttendedTrue(date);

        return attendances.stream()
                .map(att -> AttendanceDTO.builder()
                        .employee_id(att.getEmployeeId())
                        .date(att.getDate().toString())
                        .login_time(att.getLoginTime() != null ? att.getLoginTime().toString() : null)
                        .logout_time(att.getLogoutTime() != null ? att.getLogoutTime().toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    public Map<LocalDate, Integer> getOnTimeEmployees(LocalDate from_date, LocalDate to_date) {
        Map<LocalDate, Integer> res = new HashMap<>();
        LocalDate currentDate = from_date;
        while (!currentDate.isAfter(to_date)) {
            List<Object[]> result = attendanceRepository.findOnTimeEmployees(currentDate);
            if (result != null && !result.isEmpty()) {
                for (Object[] row : result) {
                    java.sql.Date sqlDate = (java.sql.Date) row[0];
                    LocalDate convertedDate = sqlDate.toLocalDate();
                    Integer count = ((Number) row[1]).intValue();
                    res.put(convertedDate, count);
                }
            } else {
                res.put(currentDate, 0);
            }
            currentDate = currentDate.plusDays(1);
        }
        return res;
    }

    public Map<LocalDate, Integer> getEmployeesMeetingShiftHours(LocalDate from_date, LocalDate to_date) {
        Map<LocalDate, Integer> res = new HashMap<>();
        LocalDate currentDate = from_date;

        while (!currentDate.isAfter(to_date)) {
            List<Object[]> result = attendanceRepository.findEmployeesMeetingShiftHours(currentDate);
            if (result != null && !result.isEmpty()) {
                for (Object[] row : result) {
                    java.sql.Date sqlDate = (java.sql.Date) row[0];
                    LocalDate convertedDate = sqlDate.toLocalDate();
                    Integer count = ((Number) row[1]).intValue();
                    res.put(convertedDate, count);
                }
            } else {
                res.put(currentDate, 0);
            }
            currentDate = currentDate.plusDays(1);
        }
        return res;
    }


    public HashMap<String, Duration> getOvertimeBetweenDates(LocalDate start, LocalDate end, Long limit) {
        List<Object[]> results = attendanceRepository.overtimeEmployeesBetweenDates(start, end, limit);

        HashMap<String, Duration> overtimeMap = new HashMap<>();
        for (Object[] row : results) {
            String employeeId = (String) row[0];
            Double totalOvertimeSeconds = ((Number) row[1]).doubleValue();
            Duration totalOvertime = Duration.ofSeconds(totalOvertimeSeconds.longValue());
            overtimeMap.put(employeeId, totalOvertime);
        }

        return overtimeMap;
    }

    public LinkedHashMap<String, Duration> getTopOvertimeEmployees(LocalDate start, LocalDate end, int limit) {
        List<Object[]> results = attendanceRepository.findEmployeeEffectiveAndShiftTimes(start, end);

        // employee_id → total overtime
        Map<String, Duration> overtimeMap = new HashMap<>();

        for (Object[] row : results) {
            String employeeId = (String) row[0];
            BigDecimal effectiveHoursNum = (BigDecimal) row[1];
            Duration effectiveHours = Duration.ofSeconds(effectiveHoursNum != null ?effectiveHoursNum.longValue(): 0L);
            Time startTimestamp = (Time) row[3];
            Time endTimestamp = (Time) row[4];

            LocalTime startTime = startTimestamp.toLocalTime();
            LocalTime endTime = endTimestamp.toLocalTime();

            Duration shiftDuration = Duration.between(startTime, endTime);
            Duration overtime = effectiveHours.minus(shiftDuration);

            if (overtime.isNegative()) {
                overtime = Duration.ZERO; // no negative overtime
            }

            overtimeMap.merge(employeeId, overtime, Duration::plus);
        }

        // Sort by overtime descending and limit top N
        return overtimeMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Duration>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public LinkedHashMap<String, Integer> getTopOntimeEmployees(LocalDate start, LocalDate end, int limit) {
        List<Object[]> results = attendanceRepository.findEmployeeEffectiveAndShiftTimes(start, end);

        // employee_id → total overtime
        Map<String, Integer> ontimeMap = new HashMap<>();

        for (Object[] row : results) {
            String employeeId = (String) row[0];
            Timestamp loginTime = (Timestamp) row[2];
            LocalDateTime login = loginTime.toLocalDateTime();
            Time startTimestamp = (Time) row[3];
            Time endTimestamp = (Time) row[4];

            LocalTime startTime = startTimestamp.toLocalTime();
            
            if(startTime.isAfter(login.toLocalTime())) {
                ontimeMap.put(employeeId, ontimeMap.getOrDefault(employeeId, 0)+1);
            }
        }

        // Sort by overtime descending and limit top N
        return ontimeMap.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    public List<AttendanceSummaryDTO> getCombinedSummary(LocalDate start, LocalDate end) {

        // Fetch data
        List<Object[]> attendanceResults = attendanceRepository.getPresentAndAbsentEmployees(start, end);
        List<Object[]> leaveResults = leaveRepo.getLeaveSummary(start, end);

        // Use a map keyed by date to merge both datasets
        Map<LocalDate, AttendanceSummaryDTO> summaryMap = new HashMap<>();

        // Fill attendance data
        for (Object[] row : attendanceResults) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            AttendanceSummaryDTO dto = summaryMap.getOrDefault(date, new AttendanceSummaryDTO());
            dto.setDate(date);
            dto.setPresent(((Number) row[1]).intValue());
            dto.setAbsent(((Number) row[2]).intValue());
            summaryMap.put(date, dto);
        }

        // Fill leave data
        for (Object[] row : leaveResults) {
            LocalDate date = ((java.sql.Date) row[0]).toLocalDate();
            AttendanceSummaryDTO dto = summaryMap.getOrDefault(date, new AttendanceSummaryDTO());
            dto.setDate(date);
            dto.setPaidApprovedLeaves(((Number) row[1]).intValue());
            dto.setPaidUnapprovedLeaves(((Number) row[2]).intValue());
            dto.setUnpaidApprovedLeaves(((Number) row[3]).intValue());
            dto.setUnpaidUnapprovedLeaves(((Number) row[4]).intValue());
            dto.setSickLeaves(((Number) row[5]).intValue());
            dto.setSickUnapprovedLeaves(((Number) row[6]).intValue());
            dto.setCasualApprovedLeaves(((Number) row[7]).intValue());
            dto.setCasualUnapprovedLeaves(((Number) row[8]).intValue());
            dto.setApprovedLeaves(((Number) row[9]).intValue());
            dto.setPendingLeaves(((Number) row[10]).intValue());
            summaryMap.put(date, dto);
        }

        // Return combined results as list sorted by date
        return summaryMap.values().stream()
                .sorted(Comparator.comparing(AttendanceSummaryDTO::getDate))
                .collect(Collectors.toList());
    }

public float getWorkingDays(String employeeId, int month, int year, WorkingDaysType type) {
    LocalDate startDate = LocalDate.of(year, month, 1);
    LocalDate endDate = startDate.plusMonths(1).minusDays(1);

    List<Object[]> result = attendanceRepository.getPresentAndAbsentByEmployee(employeeId, startDate, endDate);

    if (result.isEmpty()) {
        return 0; // no attendance data
    }

    Object[] row = result.getFirst();
    int present = ((Number) row[1]).intValue();
    int absent = ((Number) row[2]).intValue();
    int first_half = ((Number) row[3]).intValue();
    int second_half = ((Number) row[4]).intValue();
    int consider_present = ((Number) row[5]).intValue();
    float actualPresent = (float) consider_present + (float) (first_half + second_half)/2;
    if (type == WorkingDaysType.PRESENT) {
        return actualPresent;
    } else if (type == WorkingDaysType.ABSENT) {
        return absent;
    } else {
        return present + absent;
    }
}
}
