package com.example.employee.Repository;

import com.example.employee.entity.TicketReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketReplyRepository extends JpaRepository<TicketReply, Long> {

    Page<TicketReply> findByTicket_TicketId(String ticketId, Pageable pageable);
}
