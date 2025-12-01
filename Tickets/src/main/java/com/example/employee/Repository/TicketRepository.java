package com.example.employee.Repository;


import com.example.employee.entity.Roles;
import com.example.employee.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, String> {

    Page<Ticket> findByEmployeeId(String employeeId, Pageable pageable);

    Page<Ticket> findByRolesAndEmployeeId(Roles roles, String employeeId, Pageable pageable);
    Page<Ticket> findByRoles(Roles roles, Pageable pageable);

}

