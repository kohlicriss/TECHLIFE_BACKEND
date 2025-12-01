package com.example.employee.dto;


import com.example.employee.entity.Roles;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketReplyDTO {

    private String ticketId;
    private String replyText;
    private String status;
    private String priority;
    private String title;

    private String employeeId;
    private String repliedBy;


   // @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    //private LocalDateTime repliedAt;
   private String repliedAt;
    private Roles roles;

}
