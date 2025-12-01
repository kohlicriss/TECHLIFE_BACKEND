package com.example.RealTime_Attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.RealTime_Attendance.Entity.Break;

@Repository
public interface BreaksRepository extends JpaRepository<Break, Long> {
}