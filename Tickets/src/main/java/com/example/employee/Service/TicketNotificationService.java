package com.example.employee.Service;


import com.example.employee.client.NotificationClient;
import com.example.employee.entity.NotificationRequest;
import com.example.employee.entity.Roles;
import com.example.employee.entity.Ticket;
import com.example.employee.entity.User;
import com.example.employee.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketNotificationService {

    private final NotificationClient notificationClient;
    private final UserRepository userRepository;
    private final AsyncTicketHelper asyncTaskService;


    public void sendTicketAssignmentNotification(Ticket ticket) {
        try {
            Roles assignedRole = ticket.getRoles();
            List<User> usersWithRole = userRepository.findByRoles(assignedRole);

            for (User user : usersWithRole) {

                if (user.getEmployeeId().equals(ticket.getEmployeeId())) continue;

                NotificationRequest notification = NotificationRequest.builder()
                        .receiver(user.getEmployeeId())
                        .sender(ticket.getEmployeeId())
                        .message("A new ticket has been assigned to your role: " + ticket.getTitle())
                        .link("/tickets/" + ticket.getTicketId())
                        .type("INFO")
                        .category("TICKET")
                        .kind("TICKET_ASSIGNED")
                        .subject("New Ticket Assigned")
                        .build();

                log.info("Sending ticket assignment notification to user={}", user.getEmployeeId());
                asyncTaskService.sendNotificationAsync(notification);
            }
        } catch (Exception e) {
            log.error("Failed to send ticket assignment notification for ticket {}: {}",
                    ticket.getTicketId(), e.getMessage(), e);
        }
    }


    public void sendAdminReplyNotification(Ticket ticket, String repliedBy, String replyText) {
        try {

            NotificationRequest notification = NotificationRequest.builder()
                    .receiver(ticket.getEmployeeId())
                    .sender(repliedBy)
                    .message(getRoleDisplayName(repliedBy) + " replied to your ticket: " + ticket.getTitle() +
                            ". Reply: " + (replyText.length() > 50 ? replyText.substring(0, 50) + "..." : replyText))
                    .link("/tickets/" + ticket.getTicketId())
                    .type("INFO")
                    .category("TICKET")
                    .kind("REPLY_RECEIVED")
                    .subject("New Reply on Your Ticket")

                    .build();

            log.info("Sending admin reply notification to employee={}", ticket.getEmployeeId());
            asyncTaskService.sendNotificationAsync(notification);

        } catch (Exception e) {
            log.error("Failed to send admin reply notification for ticket {}: {}",
                    ticket.getTicketId(), e.getMessage(), e);
        }
    }


    public void sendEmployeeReplyNotification(Ticket ticket, String repliedBy, String replyText) {
        try {
            Roles assignedRole = ticket.getRoles();
            List<User> usersWithRole = userRepository.findByRoles(assignedRole);

            for (User user : usersWithRole) {

                if (user.getEmployeeId().equals(repliedBy)) continue;

                NotificationRequest notification = NotificationRequest.builder()
                        .receiver(user.getEmployeeId())
                        .sender(repliedBy)
                        .message("Employee replied on ticket: " + ticket.getTitle() +
                                ". Reply: " + (replyText.length() > 50 ? replyText.substring(0, 50) + "..." : replyText))
                        .link("/tickets/" + ticket.getTicketId())
                        .type("INFO")
                        .category("TICKET")
                        .kind("EMPLOYEE_REPLY")
                        .subject("Employee Replied to Ticket")
                        //.createdAt(LocalDateTime.now())
                        .build();

                log.info("Sending employee reply notification to {} for ticket {}",
                        user.getEmployeeId(), ticket.getTicketId());
                asyncTaskService.sendNotificationAsync(notification);
            }
        } catch (Exception e) {
            log.error("Failed to send employee reply notification for ticket {}: {}",
                    ticket.getTicketId(), e.getMessage(), e);
        }
    }


    public void sendTicketResolvedNotification(Ticket ticket, String resolvedBy) {
        try {

            NotificationRequest notification = NotificationRequest.builder()
                    .receiver(ticket.getEmployeeId())
                    .sender(resolvedBy)
                    .message("Your ticket '" + ticket.getTitle() + "' has been resolved by " + getRoleDisplayName(resolvedBy))
                    .link("/tickets/" + ticket.getTicketId())
                    .type("SUCCESS")
                    .category("TICKET")
                    .kind("TICKET_RESOLVED")
                    .subject("Ticket Resolved")
                    //.createdAt(LocalDateTime.now())
                    .build();

            log.info("Sending ticket resolved notification to employee={}", ticket.getEmployeeId());
            asyncTaskService.sendNotificationAsync(notification);

        } catch (Exception e) {
            log.error("Failed to send ticket resolved notification for ticket {}: {}",
                    ticket.getTicketId(), e.getMessage(), e);
        }
    }


    public void sendTicketStatusUpdateNotification(Ticket ticket, String updatedBy, String oldStatus, String newStatus) {
        try {
            // Notify the employee who created the ticket
            NotificationRequest notification = NotificationRequest.builder()
                    .receiver(ticket.getEmployeeId())
                    .sender(updatedBy)
                    .message("Your ticket '" + ticket.getTitle() + "' status changed from " +
                            oldStatus + " to " + newStatus)
                    .link("/tickets/" + ticket.getTicketId())
                    .type("INFO")
                    .category("TICKET")
                    .kind("STATUS_UPDATE")
                    .subject("Ticket Status Updated")
                    //.createdAt(LocalDateTime.now())
                    .build();

            log.info("Sending status update notification to employee={}", ticket.getEmployeeId());
            asyncTaskService.sendNotificationAsync(notification);

        } catch (Exception e) {
            log.error("Failed to send status update notification for ticket {}: {}",
                    ticket.getTicketId(), e.getMessage(), e);
        }
    }

    private String getRoleDisplayName(String employeeId) {

        if (employeeId.contains("admin") || employeeId.contains("ADMIN")) {
            return "Admin";
        } else if (employeeId.contains("manager") || employeeId.contains("MANAGER")) {
            return "Manager";
        } else if (employeeId.contains("hr") || employeeId.contains("HR")) {
            return "HR";
        } else {
            return "Employee";
        }
    }
}