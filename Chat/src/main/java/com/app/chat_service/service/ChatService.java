package com.app.chat_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.EmployeeDepartmentDTO;
import com.app.chat_service.dto.EmployeeTeamResponse;
import com.app.chat_service.dto.TeamResponse;
import com.app.chat_service.handler.IlleagalArgumentsException;
import com.app.chat_service.handler.NotFoundException;
import com.app.chat_service.kakfa.KafkaMessageProcessorService;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    @Autowired
    private AllEmployees allEmployees;
    @Autowired
    private TeamService teamService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
   
    @Autowired
    KafkaMessageProcessorService messageProcessor;
    
    public String sendMessage(ChatMessageRequest request) {
        String type = request.getType() != null ? request.getType().toUpperCase() : "";
        String senderId = request.getSender();
        String receiverId = request.getReceiver();
        String groupId = request.getGroupId();

        if (!allEmployees.existsById(senderId)) {
            throw new NotFoundException("Sender is not found with id: {}"+senderId);
        }

        switch (type) {
            case "PRIVATE":
                if (receiverId == null || !allEmployees.existsById(receiverId)) {
                    throw new NotFoundException("Error:receiverId is not found");
                    }
                break;
            case "TEAM":
                if (groupId == null || !teamService.existsByTeamId(groupId)) {
                    throw new NotFoundException("Error:TeamId is not found");
                }
                if (!isEmployeeInTeam(senderId, groupId)) {
                    throw new NotFoundException("Sender is not a part of the team");
                }
                break;
           
            default:
                throw new IlleagalArgumentsException("Invalid return type");
        }


        ChatMessage message = buildMessage(request, type);
        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("✅ Message saved to DB with ID: {}", message.getId());

//        if ("PRIVATE".equals(type)) {
//            messagingTemplate.convertAndSendToUser(receiverId, "/queue/messages", message);
//        } else {
//            messagingTemplate.convertAndSend("/topic/group/" + groupId, message);
//        }
        if (request.getClientId() != null) {
            String ackQueue = "PRIVATE".equalsIgnoreCase(savedMessage.getType())
                    ? "/queue/private-ack"
                    : "/queue/group-ack";
            log.info(ackQueue + " recived");
            messagingTemplate.convertAndSendToUser(
                    savedMessage.getSender(),
                    ackQueue,
                    savedMessage
            );

            log.info("✅ Sent ACK for message ID {} to sender {} on queue {}",
                    savedMessage.getId(), savedMessage.getSender(), ackQueue);
        }

        messageProcessor.processChatMessage(message);

        return "Message sent successfully to the Message Processer.";
    }

    private ChatMessage buildMessage(ChatMessageRequest request, String type) {
        ChatMessage message = new ChatMessage();
        message.setSender(request.getSender());
        message.setReceiver("PRIVATE".equals(type) ? request.getReceiver() : null);
        message.setGroupId(("TEAM".equals(type) || "DEPARTMENT".equals(type)) ? request.getGroupId() : null);
        message.setType(type);
        message.setContent(request.getContent() != null ? request.getContent() : "");
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

    private boolean isEmployeeInTeam(String employeeId, String teamId) {
        TeamResponse team = teamService.getGroupById(teamId);
        return team != null && team.getEmployees() != null &&
               team.getEmployees().stream().anyMatch(emp -> employeeId.equals(emp.getEmployeeId()));
    }

    // --- Cached methods (fixed sync/unless issue) ---
    @Transactional(readOnly = true)
    public List<ChatMessage> getTeamMessages(String teamId) {
        return chatMessageRepository.findByGroupIdAndType(teamId, "TEAM");
    }
    

    @Transactional(readOnly = true)
    public List<ChatMessage> getPrivateChatHistory(String sender, String receiver) {
        return chatMessageRepository.findBySenderAndReceiverOrReceiverAndSender(sender, receiver, sender, receiver);
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getGroupChatHistory(String groupId) {
        return chatMessageRepository.findByGroupId(groupId);
    }

}
