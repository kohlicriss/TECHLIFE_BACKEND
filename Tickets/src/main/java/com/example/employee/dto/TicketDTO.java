package com.example.employee.dto;



import com.example.employee.entity.Roles;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TicketDTO {
    private String ticketId;
    private String title;
    private String description;
    private String status;
    private String priority;
    private String employeeId;
    private String sentAt;
    private Roles roles;





    private List<TicketReplyDTO> replies;


}

