package com.example.OfferLetter.Service;


import com.example.OfferLetter.Client.NotificationClient;
import com.example.OfferLetter.Dto.NotificationRequest;
import com.example.OfferLetter.Entity.Employee;
import com.example.OfferLetter.Entity.Payroll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationClient notificationClient;

    public void sendSalaryUploadNotification(Payroll payroll, Employee employee) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .receiver(employee.getEmployeeId())
                    .subject("Salary Processed Successfully")
                    .message(String.format("Your salary for %d/%d has been processed. Net Amount: ₹%.2f",
                            payroll.getMonth(), payroll.getYear(), payroll.getNetSalary()))
                    .sender("payroll-system")
                    .type("INFO")
                    .category("PAYROLL")
                    .kind("SALARY_PROCESSED")
                    //.link(String.format("/payroll/details/%d", payroll.getId()))
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationClient.send(notification);
            log.info("Salary upload notification sent to employee: {}", employee.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to send salary upload notification for employee: {}", employee.getEmployeeId(), e);
        }
    }

    public void sendBonusAddedNotification(Payroll payroll, Employee employee, Double bonusAmount) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .receiver(employee.getEmployeeId())
                    .subject("Bonus Added to Your Salary")
                    .message(String.format("A bonus of ₹%.2f has been added to your salary for %d/%d. Total Bonus: ₹%.2f",
                            bonusAmount, payroll.getMonth(), payroll.getYear(), payroll.getBonusAmount()))
                    .sender("payroll-system")
                    .type("SUCCESS")
                    .category("PAYROLL")
                    .kind("BONUS_ADDED")
                    //.link(String.format("/payroll/details/%d", payroll.getId()))
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationClient.send(notification);
            log.info("Bonus added notification sent to employee: {}", employee.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to send bonus added notification for employee: {}", employee.getEmployeeId(), e);
        }
    }

    public void sendBonusUpdatedNotification(Payroll payroll, Employee employee, Double oldBonus, Double newBonus) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .receiver(employee.getEmployeeId())
                    .subject("Bonus Added")
                    .message(String.format("Your bonus for %d/%d has been added from ₹%.2f to ₹%.2f",
                            payroll.getMonth(), payroll.getYear(), oldBonus, newBonus))
                    .sender("payroll-system")
                    .type("INFO")
                    .category("PAYROLL")
                    .kind("BONUS_UPDATED")
                    //.link(String.format("/payroll/details/%d", payroll.g))
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationClient.send(notification);
            log.info("Bonus updated notification sent to employee: {}", employee.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to send bonus updated notification for employee: {}", employee.getEmployeeId(), e);
        }
    }

    public void sendSalaryRecalculatedNotification(Payroll payroll, Employee employee) {
        try {
            NotificationRequest notification = NotificationRequest.builder()
                    .receiver(employee.getEmployeeId())
                    .subject("Salary Recalculated")
                    .message(String.format("Your salary for %d/%d has been recalculated. New Net Amount: ₹%.2f",
                            payroll.getMonth(), payroll.getYear(), payroll.getNetSalary()))
                    .sender("payroll-system")
                    .type("INFO")
                    .category("PAYROLL")
                    .kind("SALARY_RECALCULATED")
                    .link("/payroll")
                    .createdAt(LocalDateTime.now())
                    .build();

            notificationClient.send(notification);
            log.info("Salary recalculated notification sent to employee: {}", employee.getEmployeeId());
        } catch (Exception e) {
            log.error("Failed to send salary recalculated notification for employee: {}", employee.getEmployeeId(), e);
        }
    }
}