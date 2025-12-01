package com.example.employee.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_reply", schema = "ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "ticket_Id")
    @JsonBackReference
    @ToString.Exclude 
    private Ticket ticket;


    private String replyText;
    private String status;
    private String priority;
    private String name;

    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "replied_by")
    private String repliedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime repliedAt;

    @Enumerated(EnumType.STRING)
    private Roles roles;

}

