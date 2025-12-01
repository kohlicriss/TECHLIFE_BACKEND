package com.example.OfferLetter.Repository;

import com.example.OfferLetter.Entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    List<Payroll> findByEmployeeEmployeeId(String employeeId);
    Optional<Payroll> findByEmployeeEmployeeIdAndMonthAndYear(String employeeId, Integer month, Integer year);
    List<Payroll> findByMonthAndYear(Integer month, Integer year);
}
