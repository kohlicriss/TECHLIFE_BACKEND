package com.example.RealTime_Attendance.repository;
import com.example.RealTime_Attendance.Entity.LeaveEntity;
import com.example.RealTime_Attendance.Entity.PersonalLeaves;

import com.example.RealTime_Attendance.Entity.Shifts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonalLeavesRepository extends JpaRepository<PersonalLeaves, Long> {

    Optional<PersonalLeaves> findByEmployeeIdAndMonthAndYear(String employeeId,Integer month,Integer year);

    void deleteByEmployeeIdAndMonthAndYear(String employeeId,Integer month,Integer year);

    @Query(nativeQuery = true, value = "select DISTINCT  employee_id FROM attendance.personal_leaves")
    Optional<List<String>> getAllEmployees();

    Optional<List<PersonalLeaves>> findByEmployeeId(String employeeId);

    boolean existsByEmployeeIdAndMonthAndYear(String employeeId, Integer month, Integer year);

    Optional<Page<PersonalLeaves>> findByMonthAndYear(int month, int year, Pageable pageable);
    Optional<List<PersonalLeaves>> findByMonthAndYear(int month, int year );
    List<PersonalLeaves> findByShifts(Shifts shifts);
    @Query("SELECT p FROM PersonalLeaves p WHERE p.shifts.shiftName = :shiftName")
    Optional<Page<PersonalLeaves>> findByShiftName(String shiftName, Pageable pageable);

    Optional<Page<PersonalLeaves>> findByEmployeeId(String employeeId, Pageable pageable);

}
