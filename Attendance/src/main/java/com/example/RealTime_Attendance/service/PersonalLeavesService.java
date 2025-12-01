package com.example.RealTime_Attendance.service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.RealTime_Attendance.Dto.AttendanceRequest;
import com.example.RealTime_Attendance.Dto.PersonDetailsAddDto;
import com.example.RealTime_Attendance.Entity.PersonalLeaves;
import com.example.RealTime_Attendance.Entity.Shifts;
import com.example.RealTime_Attendance.Exception.CustomException;
import com.example.RealTime_Attendance.repository.PersonalLeavesRepository;
import com.example.RealTime_Attendance.repository.ShiftsRepo;
import com.example.RealTime_Attendance.Enums.WorkingDaysType;


import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PersonalLeavesService {
  
  @Autowired
  private PersonalLeavesRepository personalLeavesRepository;
  @Autowired
  private ShiftsRepo shiftsRepository;
  @Autowired
  private AttendanceService attendanceService;


  public String addLeaves(PersonDetailsAddDto leave) {
    Optional<Shifts> shift = shiftsRepository.findById(leave.getShiftName());
    if(!shift.isPresent()){
      log.info("no shift fount with name {}", leave.getShiftName());
      throw new CustomException("Shift not found with name: " + leave.getShiftName(), HttpStatus.NOT_FOUND);
    }
    if (personalLeavesRepository.findByEmployeeIdAndMonthAndYear(leave.getEmployeeId(), leave.getMonth(), leave.getYear()).isPresent()) {
      log.info("leave already exists for employeeId {}", leave.getEmployeeId());
      throw new CustomException("Leave already exists for employeeId: " + leave.getEmployeeId(), HttpStatus.ALREADY_REPORTED);
    }
    PersonalLeaves person = PersonalLeaves.builder()
                            .employeeId(leave.getEmployeeId())
                            .month(leave.getMonth())
                            .year(leave.getYear())
                            .paid(leave.getPaid())
                            .paidConsumed(leave.getPaidConsumed()!= null ? leave.getPaidConsumed() : Long.valueOf(0))
                            .casual(leave.getCasual())
                            .casualConsumed(leave.getCasualConsumed()!= null ? leave.getCasualConsumed() : Long.valueOf(0))
                            .sick(leave.getSick())
                            .sickConsumed(leave.getSickConsumed()!= null ? leave.getSickConsumed() : Long.valueOf(0))
                            .unpaid(leave.getUnpaid())
                            .unpaidConsumed(leave.getUnpaidConsumed()!= null ? leave.getUnpaidConsumed() : Long.valueOf(0))
                            .shifts(shift.get())
                            .build();
    personalLeavesRepository.save(person);
    return "Leaves added successfully";
  }

  public String updateLeaves(PersonDetailsAddDto leave, Integer month, Integer year) {
    Optional<PersonalLeaves> person = personalLeavesRepository.findByEmployeeIdAndMonthAndYear(leave.getEmployeeId(), month, year);
    if(!person.isPresent()){
      log.info("no leave fount with id {}", leave.getEmployeeId());
      throw new RuntimeException("Leave not found with id: " + leave.getEmployeeId());
    }
    person.get().setPaid(leave.getPaid()!= null ? leave.getPaid() : person.get().getPaid());
    person.get().setPaidConsumed(leave.getPaidConsumed()!= null ? leave.getPaidConsumed() : person.get().getPaidConsumed());
    person.get().setCasual(leave.getCasual()!= null ? leave.getCasual() : person.get().getCasual());
    person.get().setCasualConsumed(leave.getCasualConsumed()!= null ? leave.getCasualConsumed() : person.get().getCasualConsumed());
    person.get().setSick(leave.getSick()!= null ? leave.getSick() : person.get().getSick());
    person.get().setSickConsumed(leave.getSickConsumed()!= null ? leave.getSickConsumed() : person.get().getSickConsumed());
    person.get().setUnpaid(leave.getUnpaid()!= null ? leave.getUnpaid() : person.get().getUnpaid());
    person.get().setUnpaidConsumed(leave.getUnpaidConsumed()!= null ? leave.getUnpaidConsumed() : person.get().getUnpaidConsumed());
    personalLeavesRepository.save(person.get());
    return "Leaves updated successfully";
  }

  public String deleteLeaves(String EmployeeId, Integer month, Integer year) {
    personalLeavesRepository.deleteByEmployeeIdAndMonthAndYear(EmployeeId, month, year);
    return "Leaves deleted successfully";
  }

  public PersonDetailsAddDto getLeaves(String employee_id, Integer month, Integer year) {
    Optional<PersonalLeaves> person = personalLeavesRepository.findByEmployeeIdAndMonthAndYear(employee_id, month, year);
    if(!person.isPresent()){
      log.info("got no leave found for employeeId {}", employee_id);
      throw new CustomException("Leave not found for id: " + employee_id, HttpStatus.NOT_FOUND);
    }
      return PersonDetailsAddDto.builder()
                              .employeeId(person.get().getEmployeeId())
                              .month(person.get().getMonth())
                              .year(person.get().getYear())
                              .paid(person.get().getPaid())
                              .paidConsumed(person.get().getPaidConsumed())
                              .casual(person.get().getCasual())
                              .casualConsumed(person.get().getCasualConsumed())
                              .sick(person.get().getSick())
                              .sickConsumed(person.get().getSickConsumed())
                              .unpaid(person.get().getUnpaid())
                              .unpaidConsumed(person.get().getUnpaidConsumed())
                              .shiftName(person.get().getShifts().getShiftName())
                              .build();
  }

    public CompletableFuture<List<AttendanceRequest>> getLeavesByEmployee(String employee_id, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("month").ascending());

        Optional<Page<PersonalLeaves>> p = personalLeavesRepository.findByEmployeeId(employee_id, pageable);
        if(p.isEmpty()){
            log.info("got no leave found for employeeId {}", employee_id);
            throw new CustomException("Leave not found for id: " + employee_id, HttpStatus.NOT_FOUND);
        }
        List<PersonalLeaves> person1 = p.get().toList();

        List<AttendanceRequest> result = person1.stream()
                .map(person -> AttendanceRequest.builder()
                        .employeeId(person.getEmployeeId())
                        .month(person.getMonth() != null ? person.getMonth() : null)
                        .year(person.getYear() != null ? person.getYear() : null)
                        .totalWorkingDays(attendanceService.getWorkingDays(
                                person.getEmployeeId(), person.getMonth(), person.getYear(), WorkingDaysType.TOTAL))
                        .daysPresent(attendanceService.getWorkingDays(
                                person.getEmployeeId(), person.getMonth(), person.getYear(), WorkingDaysType.PRESENT))
                        .unpaidLeaves(person.getUnpaidConsumed() != null ?  person.getUnpaidConsumed().floatValue() : 0)
                        .build())
                .toList();

        return CompletableFuture.completedFuture(result);
    }
  public CompletableFuture<List<AttendanceRequest>> attendanceReport(int month, int year, int page, int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("employeeId").ascending());
      Optional<Page<PersonalLeaves>> allPersons = Optional.empty();
      List<PersonalLeaves> persons = new ArrayList<>();
      if (size != Integer.MAX_VALUE) {
          allPersons = personalLeavesRepository.findByMonthAndYear(month, year, pageable);
          if (allPersons.isEmpty()) {
              throw new CustomException("No leaves found for month: " + month + " year: " + year, HttpStatus.NOT_FOUND);
          }
          persons = allPersons.get().getContent();
      } else {
          Optional<List<PersonalLeaves>> somePersons = personalLeavesRepository.findByMonthAndYear(month, year);
          if (somePersons.isEmpty()) {
              throw new CustomException("No leaves found for year: " + year, HttpStatus.NOT_FOUND);
          }
          persons = somePersons.get();
      }

      List<AttendanceRequest> result = persons.stream()
            .map(person -> AttendanceRequest.builder()
                    .employeeId(person.getEmployeeId())
                    .month(person.getMonth() != null ? person.getMonth() : month)
                    .year(person.getYear() != null ? person.getYear() : year)
                    .totalWorkingDays(attendanceService.getWorkingDays(
                            person.getEmployeeId(), month, year, WorkingDaysType.TOTAL))
                    .daysPresent(attendanceService.getWorkingDays(
                            person.getEmployeeId(), month, year, WorkingDaysType.PRESENT))
                    .unpaidLeaves(person.getUnpaidConsumed() != null ?  person.getUnpaidConsumed().floatValue() : 0)
                    .build())
            .toList();

    return CompletableFuture.completedFuture(result);
}


}