package com.example.employee.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ticket", schema = "ticket")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @Column(name = "ticket_Id", nullable = false)
    private String ticketId;


    private String title;

    private String description;

    private String status;

    private String priority;


    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "sent_at")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sentAt;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    @Builder.Default
    @ToString.Exclude 
    private List<TicketReply> replies = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Roles roles;


}
