package com.example.RealTime_Attendance.service;

import com.example.RealTime_Attendance.Dto.AttendanceRequest;
import com.example.RealTime_Attendance.Dto.ShiftsDto;
import com.example.RealTime_Attendance.Entity.EntityMapper;
import com.example.RealTime_Attendance.Entity.PersonalLeaves;
import com.example.RealTime_Attendance.Entity.Shifts;
import com.example.RealTime_Attendance.Exception.CustomException;
import com.example.RealTime_Attendance.repository.PersonalLeavesRepository;
import com.example.RealTime_Attendance.repository.ShiftsRepo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShiftsService {

    private final ShiftsRepo shiftsRepository;

    @Autowired
    PersonalLeavesRepository personalLeavesRepository;

    @Autowired
    EntityMapper entityMapper;

    public ShiftsService(ShiftsRepo shiftsRepository) {
        this.shiftsRepository = shiftsRepository;
    }

    public Shifts saveShift(Shifts shift) {

        Optional<Shifts> checkShift = shiftsRepository.findById(shift.getShiftName());
        if (checkShift.isPresent()) {
            throw new CustomException("Shift Already Exists" + checkShift.get().getShiftName(), HttpStatus.ALREADY_REPORTED);
        }
        return shiftsRepository.save(shift);
    }

    public List<ShiftsDto> getAllShifts() {
        return shiftsRepository.findAll().stream().map(Shifts::toShiftsDto).toList();
    }
    public List<AttendanceRequest> getEmployeesInShift(String shiftName, int page, int size) {
        Optional<Page<PersonalLeaves>> personalLeavesPage = personalLeavesRepository.findByShiftName(shiftName, PageRequest.of(page, size));
        if (personalLeavesPage.isEmpty()) {throw new CustomException("Shift Not Found", HttpStatus.NOT_FOUND);}
        return personalLeavesPage.get().map(entityMapper::toAttendanceRequest).getContent();
    }

    public Shifts getShiftOfEmployee(String employeeId, int month, int year) {
        Optional<PersonalLeaves> eOp = personalLeavesRepository.findByEmployeeIdAndMonthAndYear(employeeId, month, year);
        if (eOp.isEmpty()) {throw new CustomException("Employee Not Found", HttpStatus.NOT_FOUND);}
        Shifts sh = eOp.get().getShifts();
        sh.setPersonalLeaves(null);
        return sh;
    }

    public Shifts getShiftById(String shiftName) {
        Optional<Shifts> shifts = shiftsRepository.findById(shiftName);
        return shifts.orElse(null);
    }

    public Shifts updateShift(String shiftName, Shifts updatedShift) {
        return shiftsRepository.findById(shiftName)
                .map(existingShift -> {
                    existingShift.setStartTime(updatedShift.getStartTime());
                    existingShift.setEndTime(updatedShift.getEndTime());
                    return shiftsRepository.save(existingShift);
                })
                .orElseThrow(() -> new RuntimeException("Shift not found with name: " + shiftName));
    }

    public void deleteShift(String shiftName) {
        shiftsRepository.deleteById(shiftName);
    }
}
