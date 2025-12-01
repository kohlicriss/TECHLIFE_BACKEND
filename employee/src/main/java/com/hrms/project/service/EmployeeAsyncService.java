package com.hrms.project.service;

import com.hrms.project.client.ChatEmployeeClient;
import com.hrms.project.client.NotificationClient;
import com.hrms.project.dto.ChatEmployeeDTO;
import com.hrms.project.dto.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EmployeeAsyncService {

    @Autowired
    private NotificationClient notificationClient;

    @Autowired
    private ChatEmployeeClient chatEmployeeClient;

    @Async("employeeTaskExecutor")
    public CompletableFuture<Void> sendNotification(NotificationRequest notification) {
        try {
            notificationClient.send(notification);
            log.info("Notification sent to {}", notification.getReceiver());
        } catch (Exception e) {
            log.error("Failed to send notification to {}: {}", notification.getReceiver(), e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }


    @Async("employeeTaskExecutor")
    public CompletableFuture<Void> sendNotifications(List<NotificationRequest> notifications) {
        try {
            notificationClient.sendList(notifications);
            log.info("Notifications sent to {} users", notifications.size());
        } catch (Exception e) {
            log.error("Failed to send notifications list: {}", e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    @Async("employeeTaskExecutor")
    public CompletableFuture<Void> syncWithChat(ChatEmployeeDTO chatDTO, String employeeId, boolean isCreate) {
        try {
            if (isCreate) {
                chatEmployeeClient.addEmployee(chatDTO);
            } else {
                chatEmployeeClient.updateEmployee(employeeId, chatDTO);
            }
        } catch (Exception e) {
            log.error("Failed to sync employee {} with chat: {}", employeeId, e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

}
