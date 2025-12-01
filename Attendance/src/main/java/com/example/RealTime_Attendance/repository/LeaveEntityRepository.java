package com.example.RealTime_Attendance.repository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.RealTime_Attendance.Entity.LeaveEntity;
import com.example.RealTime_Attendance.Enums.LeaveStatus;

import io.lettuce.core.dynamic.annotation.Param;

import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;



@Repository
public interface LeaveEntityRepository extends JpaRepository<LeaveEntity, Long> {

    List<LeaveEntity> findByEmployeeIdOrderByCreatedAtDesc(String employeeId);
    Page<LeaveEntity> findByEmployeeIdOrderByReqOnDesc(String employeeId, Pageable pageable);
    List<LeaveEntity> findByStatus(LeaveStatus status);
    List<LeaveEntity> findByReqToOrderByReqOn(LocalDate reqOn);

        @Query(value = "SELECT req_on, " +
            "COUNT(DISTINCT CASE WHEN status = 1 AND leave_type = 0 THEN employee_id END) AS paid_approved_leaves, " +
            "COUNT(DISTINCT CASE WHEN status = 0 AND leave_type = 0 THEN employee_id END) AS paid_unapproved_leaves, " +
            "COUNT(DISTINCT CASE WHEN status = 1 AND leave_type = 3 THEN employee_id END) AS unpaid_approved_leaves, " +
            "COUNT(DISTINCT CASE WHEN status = 0 AND leave_type = 3 THEN employee_id END) AS unpaid_unapproved_leaves, " +
            "COUNT(DISTINCT CASE WHEN status = 1 AND leave_type = 2 THEN employee_id END) AS sick_leaves, " +
            "COUNT(DISTINCT CASE WHEN status = 0 AND leave_type = 2 THEN employee_id END) AS sick_unapproves_leaves, " +
            "COUNT(DISTINCT CASE WHEN status = 1 AND leave_type = 1 THEN employee_id END) AS casual_approved_leaves, " +
            "COUNT(DISTINCT CASE WHEN status = 0 AND leave_type = 1 THEN employee_id END) AS casual_unapproves_leaves, " +
            "COUNT(DISTINCT CASE WHEN status = 1 THEN employee_id END) AS approved_leaves, " +
            "COUNT(DISTINCT CASE WHEN status = 0 THEN employee_id END) AS pending_leaves " +
            "FROM attendance.leaves " +
            "WHERE req_to::date BETWEEN :start AND :end " +
            "GROUP BY req_on",
            nativeQuery = true)
    List<Object[]> getLeaveSummary(@Param("start") LocalDate start,
                                   @Param("end") LocalDate end);
}
