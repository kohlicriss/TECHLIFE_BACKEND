package com.example.RealTime_Attendance.service;

import com.example.RealTime_Attendance.Dto.LeaveRequestDTO;
import com.example.RealTime_Attendance.Dto.LeavesDto;
import com.example.RealTime_Attendance.Dto.PersonalLeaveDetails;
import com.example.RealTime_Attendance.Dto.PersonalLeaveDetailsDTO;
import com.example.RealTime_Attendance.Entity.LeaveEntity;
import com.example.RealTime_Attendance.Entity.PersonalLeaves;
import com.example.RealTime_Attendance.Enums.LeaveStatus;
import com.example.RealTime_Attendance.Enums.LeaveType;
import com.example.RealTime_Attendance.Exception.CustomException;
import com.example.RealTime_Attendance.repository.AttendanceRepository;
import com.example.RealTime_Attendance.repository.LeaveEntityRepository;
import com.example.RealTime_Attendance.repository.PersonalLeavesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveEntityRepository leaveEntityRepository;
    private final PersonalLeavesRepository personalLeavesRepository;
    private final AttendanceRepository attendanceRepository;

    public String createLeaveRequest(LeaveRequestDTO dto) {
        
        Integer currentMonth = (Integer) LocalDate.now().getMonthValue();
        Integer currentYear = (Integer) LocalDate.now().getYear();

        PersonalLeaves balance = personalLeavesRepository
                .findByEmployeeIdAndMonthAndYear(dto.getEmployeeId(), currentMonth, currentYear)
                .orElseThrow(() -> new CustomException(
                        "Leave balance not found for employee " + dto.getEmployeeId() + " in " + currentMonth + "/" + currentYear, HttpStatus.NOT_FOUND
                ));

        LeaveType type = dto.getLeave_Type();

        switch (type) {
            case LeaveType.SICK:
                if (balance.getSick() - balance.getSickConsumed() < dto.getNumberOfDays() ) {
                    log.info("Requested number of Sick Leaves are not available total sick leaves are {} and consumed sick leaves are {}",balance.getSick(),balance.getSickConsumed());
                    throw new CustomException("Requested number of Sick Leaves are not available", HttpStatus.BAD_REQUEST);
                }
                break;
            case LeaveType.PAID:
                if (balance.getPaid() - balance.getPaidConsumed() <  dto.getNumberOfDays() ) {
                    log.info("Requested number of Paid Leaves are not available total paid leaves are {} and consumed paid leaves are {}",balance.getPaid(),balance.getPaidConsumed());
                    throw new CustomException("Requested number of Paid Leaves are not available", HttpStatus.BAD_REQUEST);
                }
                break;
            case LeaveType.CASUAL:
                if (balance.getCasual() - balance.getCasualConsumed() <  dto.getNumberOfDays() ) {
                    log.info("Requested number of Casual Leaves are not available total casual leaves are {} and consumed casual leaves are {}",balance.getCasual(),balance.getCasualConsumed());
                    throw new CustomException("Requested number of Casual Leaves are not available", HttpStatus.BAD_REQUEST);
                }
                break;
            case LeaveType.UNPAID:
                if (balance.getUnpaid() - balance.getUnpaidConsumed() <=  dto.getNumberOfDays() ) {
                    log.info("Requested number of Unpaid Leaves are not available total unpaid leaves are {} and consumed unpaid leaves are {}",balance.getUnpaid(),balance.getUnpaidConsumed());
                    throw new CustomException("Requested number of Unpaid Leaves are not available", HttpStatus.BAD_REQUEST);
                }
                break;
            default:
                log.error("Invalid leave type selected: {}", dto.getLeave_Type());
                throw new CustomException("Invalid leave type selected: " + dto.getLeave_Type(), HttpStatus.BAD_REQUEST);
        }

        LocalDate prevDate = dto.getReq_To_from();
        for(int i = 0; i < dto.getNumberOfDays(); i++) {
            LeaveEntity leaveEntity = new LeaveEntity();
            leaveEntity.setEmployeeId(dto.getEmployeeId());
            leaveEntity.setLeaveType(dto.getLeave_Type());
            leaveEntity.setReqOn(LocalDate.now());
            leaveEntity.setReqTo(prevDate);
            leaveEntity.setLeaveReason(dto.getLeave_Reason());
            leaveEntity.setStatus(LeaveStatus.PENDING);
            leaveEntity.setHalfDay(dto.isHalf_Day());
            prevDate = prevDate.plusDays(1);
            leaveEntityRepository.save(leaveEntity);
        }
        log.info("creating leave request for employee {}", dto.getEmployeeId());
        return "Leave request submitted successfully";
    }

    public List<PersonalLeaveDetailsDTO> getLeaveSummary(String employeeId) {
        YearMonth current = YearMonth.now();
        Integer month = (Integer) current.getMonthValue();
        Integer year = (Integer) current.getYear();

        PersonalLeaves leaves = personalLeavesRepository
                .findByEmployeeIdAndMonthAndYear(employeeId, month, year)
                .orElseThrow(() -> new CustomException("No leave data found for this employee.", HttpStatus.NOT_FOUND));

        List<PersonalLeaveDetailsDTO> summaries = new ArrayList<>();

        summaries.add(new PersonalLeaveDetailsDTO(
                "Casual", leaves.getCasualConsumed(), leaves.getCasual() - leaves.getCasualConsumed(), leaves.getCasual()
        ));
        summaries.add(new PersonalLeaveDetailsDTO(
                "Paid", leaves.getPaidConsumed(), leaves.getPaid() - leaves.getPaidConsumed(), leaves.getPaid()
        ));
        summaries.add(new PersonalLeaveDetailsDTO(
                "Unpaid", leaves.getUnpaidConsumed(), leaves.getUnpaid() - leaves.getUnpaidConsumed(), leaves.getUnpaid()
        ));
        summaries.add(new PersonalLeaveDetailsDTO(
                "Sick", leaves.getSickConsumed(), leaves.getSick() - leaves.getSickConsumed(), leaves.getSick()
        ));
        
        log.info("The summery of leaves for employee {} is {}", employeeId, summaries);
        return summaries;
    }
    @Async("attendanceExecutor")
    // @Cacheable(value = "leaves", key = "#employeeId + ':' + #page + ':' + #size")
    public CompletableFuture<List<LeavesDto>> getAllLeaves(String employeeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("reqOn").descending());

        System.out.println("fetching data from database");

        Page<LeaveEntity> leavePage =
                leaveEntityRepository.findByEmployeeIdOrderByReqOnDesc(employeeId, pageable);

        List<LeavesDto> list = leavePage.getContent()
                .stream()
                .map(leave -> LeavesDto.builder()
                        .Leave_type(leave.getLeaveType())
                        .Leave_on(leave.getReqOn() != null ? leave.getReqOn().toString() : null)
                        .status(leave.getStatus())
                        .Request_By(leave.getEmployeeId())
                        .Details(leave.getLeaveReason())
                        .Action_Date(leave.getApprovedOn() != null ? leave.getApprovedOn().toString() : null)
                        .Rejection_Reason(leave.getRejectionReason())
                        .Action(leave.isHalfDay() ? "Half Day" : "Full Day")
                        .build())
                .toList();
        log.info("getting all the leaves for employee {}", employeeId);

        return CompletableFuture.completedFuture(list);
    }

    public void initializeMonthlyLeaveBalances() {
        YearMonth current = YearMonth.now();
        Integer month = (Integer) current.getMonthValue();
        Integer year = (Integer) current.getYear();

        List<String> employeeIds = attendanceRepository.findAllEmployeeIds();

        for (String empId : employeeIds) {
            boolean exists = personalLeavesRepository
                    .existsByEmployeeIdAndMonthAndYear(empId, month, year);

            if (!exists) {
                PersonalLeaves newLeave = PersonalLeaves.builder()
                        .employeeId(empId)
                        .month(month)
                        .year(year)
                        .casual(2.0)
                        .paid(1.0)
                        .sick(2.0)
                        .unpaid(0.0)
                        .casualConsumed(0.0)
                        .paidConsumed(0.0)
                        .sickConsumed(0.0)
                        .unpaidConsumed(0.0)
                        .build();

                personalLeavesRepository.save(newLeave);
            }
        }

        System.out.println("Leave balances initialized for month: " + month + ", year: " + year);
    }

    public PersonalLeaves addLeaves(PersonalLeaves leav) {
        return personalLeavesRepository.save(leav);
    }

    public List<PersonalLeaveDetails> initialLeaveTypeData(String empId) {
        List<PersonalLeaveDetails> leaves = new ArrayList<>();

        Optional<PersonalLeaves> pl = personalLeavesRepository
                .findByEmployeeIdAndMonthAndYear(
                        empId,
                        (Integer) LocalDateTime.now().getMonthValue(),
                        (Integer) LocalDateTime.now().getYear()
                );

        PersonalLeaveDetails pld1 = new PersonalLeaveDetails();
        PersonalLeaveDetails pld2 = new PersonalLeaveDetails();
        PersonalLeaveDetails pld3 = new PersonalLeaveDetails();
        PersonalLeaveDetails pld4 = new PersonalLeaveDetails();

        pld1.setLeaveType("Sick Leave");
        pld1.setEmployee(empId);
        pld1.setDays(pl.isPresent() ? pl.get().getSick() : 0);
        leaves.add(pld1);

        pld2.setLeaveType("Paid Leave");
        pld2.setEmployee(empId);
        pld2.setDays(pl.isPresent() ? pl.get().getPaid() : 0);
        leaves.add(pld2);

        pld3.setLeaveType("Unpaid Leave");
        pld3.setEmployee(empId);
        pld3.setDays(pl.isPresent() ? pl.get().getUnpaid() : 0);
        leaves.add(pld3);

        pld4.setLeaveType("Casual Leave");
        pld4.setEmployee(empId);
        pld4.setDays(pl.isPresent() ? pl.get().getSick() : 0);
        leaves.add(pld4);

        return leaves;
    }
    @Async("attendanceExecutor")
    public CompletableFuture<List<LeaveEntity>> getPendingLeaves() {
        List<LeaveEntity> leaves = leaveEntityRepository.findByStatus(LeaveStatus.PENDING);
        return CompletableFuture.completedFuture(leaves);
    }


    public LeaveEntity approveLeave(Long leaveId) {
        Integer currentMonth = (Integer) LocalDate.now().getMonthValue();
        Integer currentYear = (Integer) LocalDate.now().getYear();

        LeaveEntity leave = leaveEntityRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found with ID: " + leaveId));

        PersonalLeaves update = personalLeavesRepository
                .findByEmployeeIdAndMonthAndYear(leave.getEmployeeId(), currentMonth, currentYear)
                .orElseThrow(() -> new RuntimeException(
                        "Leave balance not found for employeeId: " + leave.getEmployeeId()
                ));

        switch (leave.getLeaveType()) {
            case LeaveType.PAID:
                update.setPaid(update.getPaid() - 1);
                update.setPaidConsumed(update.getPaidConsumed() + 1);
                break;
            case LeaveType.SICK:
                update.setSick(update.getSick() - 1);
                update.setSickConsumed(update.getSickConsumed() + 1);
                break;
            case LeaveType.CASUAL:
                update.setCasual(update.getCasual() - 1);
                update.setCasualConsumed(update.getCasualConsumed() + 1);
                break;
            case LeaveType.UNPAID:
                update.setUnpaid(update.getUnpaid() - 1);
                update.setUnpaidConsumed(update.getUnpaidConsumed() + 1);
                break;
        }

        leave.setStatus(LeaveStatus.APPROVED);
        leave.setRejectionReason(null);
        leave.setApprovedOn(LocalDate.now());

        personalLeavesRepository.save(update);
        return leaveEntityRepository.save(leave);
    }

    public LeaveEntity rejectLeave(Long leaveId, String reason) {
        LeaveEntity leave = leaveEntityRepository.findById(leaveId)
                .orElseThrow(() -> new RuntimeException("Leave not found with ID: " + leaveId));

        leave.setStatus(LeaveStatus.REJECTED);
        leave.setRejectionReason(reason);

        return leaveEntityRepository.save(leave);
    }
}
