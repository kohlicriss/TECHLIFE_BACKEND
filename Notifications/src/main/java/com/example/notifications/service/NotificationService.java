package com.example.notifications.service;

import com.example.notifications.dtos.EmployeeDepartmentDTO;
import com.example.notifications.dtos.EmployeeTeamResponse;
import com.example.notifications.dtos.TeamResponse;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;

import com.example.notifications.producer.NotificationProducer;
import com.example.notifications.entity.Notification;
import com.example.notifications.repository.NotificationRepository;

import com.example.notifications.clients.TeamClient;
import com.example.notifications.clients.DepartmentClient;

import org.springframework.data.domain.Pageable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private NotificationProducer producer;

    @Autowired
    private TeamClient teamClient;

    @Autowired
    private DepartmentClient departmentClient;

    public void sendNotification(String receiver, String message, String sender, String type, String link,
            String category, String kind, String subject) {
        if ("team".equalsIgnoreCase(category)) {
            TeamResponse team = teamClient.getEmployeesInTeam(receiver);
            log.info("sending notification for team {}", team);
            if (team != null && team.getEmployees() != null) {
                team.getEmployees().forEach(emp -> {
                    String employeeId = emp.getEmployeeId();

                    Notification notification = Notification.builder()
                            .receiver(employeeId)
                            .message(message)
                            .sender(sender)
                            .type(type)
                            .link(link)
                            .read(false)
                            .createdAt(LocalDateTime.now())
                            .category(category)
                            .kind(kind)
                            .subject(subject)
                            .stared(false)
                            .deleted(false)
                            .build();

                    sendNotificationAsync(notification);
                });
            }

        }
        if ("department".equalsIgnoreCase(category)) {
            EmployeeDepartmentDTO department = departmentClient.getEmployeesInDepartment(receiver);
            System.out.println(department);
            if (department != null && department.getEmployeeList() != null) {
                log.info("sending notification for department {}", department);
                for (EmployeeTeamResponse emp : department.getEmployeeList()) {
                    System.out.println("not enter into for loop");
                    String employeeId = emp.getEmployeeId();

                    Notification notification = Notification.builder()
                            .receiver(employeeId)
                            .message(message)
                            .sender(sender)
                            .type(type)
                            .link(link)
                            .read(false)
                            .createdAt(LocalDateTime.now())
                            .category(category)
                            .kind(kind)
                            .subject(subject)
                            .stared(false)
                            .deleted(false)
                            .build();

                    sendNotificationAsync(notification);
                }
            }
        }

        else {
            log.info("sending notification to the employee {}", receiver);
            Notification notification = Notification.builder()
                    .receiver(receiver)
                    .message(message)
                    .sender(sender)
                    .type(type)
                    .link(link)
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .category(category)
                    .kind(kind)
                    .subject(subject)
                    .stared(false)
                    .deleted(false)
                    .build();

            sendNotificationAsync(notification);
        }
    }

    @Async("notificationExecutor")
    @Transactional
    public void sendNotificationAsync(Notification notification) {
        evictCache(notification.getReceiver());
        repository.save(notification);
        producer.sendNotification(notification);
    }

    @Cacheable(value = "unreadNotifications", key = "#receiver")
    @Transactional
    public List<Notification> getUnreadNotifications(String receiver, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Notification> notifications = repository.findByReceiverAndReadFalse(receiver, pageable);
        return notifications.getContent();
    }

    public void stardMessage(Long id) {
        repository.findById(id).ifPresent(notification -> {
            notification.setStared(true);
            repository.save(notification);
            evictCache(notification.getReceiver());
        });
    }

    public void unstardMessage(Long id) {
        repository.findById(id).ifPresent(notification -> {
            notification.setStared(false);
            repository.save(notification);
            evictCache(notification.getReceiver());
        });
    }

    @Transactional
    public boolean deleteMessage(Long id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setDeleted(true);
        repository.save(notification);

        return true;
    }

    @Cacheable(value = "getAllNotifications", key = "#receiver + #page + #size")
    @Transactional
    public List<Notification> getAllNotifications(String receiver, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("created_at").descending());
        Page<Notification> notification = repository.findNonChatNotificationsByReceiver(receiver, pageable);
        return notification.getContent();
    }

    public void markAsRead(Long id) {
        repository.findById(id).ifPresent(notification -> {
            notification.setRead(true);
            repository.save(notification);
            evictCache(notification.getReceiver());
        });
    }

    @CachePut(value = "unreadCount", key = "#receiver")
    @Transactional
    public Long getUnreadCount(String receiver) {
        return repository.countNonChatUnreadByReceiver(receiver);
    }

    @CacheEvict(value = { "unreadNotifications", "getAllNotifications", "unreadCount" }, key = "#receiver")
    public void evictCache(String receiver) {
        Set<String> keys = redisTemplate.keys("getAllNotifications::" + receiver + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        keys = redisTemplate.keys("unreadNotifications::" + receiver + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        keys = redisTemplate.keys("unreadCount::" + receiver + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        log.info("Deleted Redis cache for receiver: " + receiver);
    }

    public List<Notification> deletedMessage() {
        return repository.findByDeletedTrue();
    }

}
