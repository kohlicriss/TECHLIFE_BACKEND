package com.example.employee.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    private String receiver;       
    private String message;         
    private String sender;          
    private String type;           
    private String link;            
    private String category;        
    private String kind;            
    private String subject;         
    private boolean read = false;
    private boolean stared = false;
    private boolean deleted = false;
  //  private LocalDateTime createdAt = LocalDateTime.now();
}