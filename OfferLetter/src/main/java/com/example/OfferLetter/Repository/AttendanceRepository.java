package com.example.OfferLetter.Repository;

import com.example.OfferLetter.Entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    @Query("SELECT a FROM Attendance a WHERE a.employee.employeeId = :employeeId AND a.month = :month AND a.year = :year")
    Optional<Attendance> findByEmployeeAndMonthYear(@Param("employeeId") String employeeId,
                                                    @Param("month") Integer month,
                                                    @Param("year") Integer year);

    List<Attendance> findByEmployeeEmployeeId(String employeeId);
}
