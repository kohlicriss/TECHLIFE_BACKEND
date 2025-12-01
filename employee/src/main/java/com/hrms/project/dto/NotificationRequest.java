package com.hrms.project.dto;

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
    private String receiver;        // e.g., "ACS00000001"
    private String message;         // e.g., "Your request has been approved."
    private String sender;          // e.g., "admin@example.com"
    private String type;            // e.g., "INFO"
    private String link;            // e.g., "https://example.com/notifications/123"
    private String category;        // e.g., "department"
    private String kind;            // e.g., "APPROVAL"
    private String subject;         // e.g., "Approval Notification"
    private boolean read = false;
    private boolean stared = false;
    private boolean deleted = false;
    private LocalDateTime createdAt = LocalDateTime.now();
}
