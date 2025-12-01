package com.app.chat_service.service;
import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.feignclient.NotificationClient;
import com.app.chat_service.handler.*;
import com.app.chat_service.dto.ForwardTarget;
import com.app.chat_service.dto.NotificationRequest;
import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatForwardService {
    private final ChatMessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TeamService teamService;
    private final NotificationClient notificationClient;
    private final AllEmployees allEmployees;
    
    @Transactional
    public void handleReplyOrForward(ReplyForwardMessageDTO dto) {
    	
        if (dto.getReplyToMessageId() != null) {
            handleReply(dto);
        } else if (dto.getForwardMessageId() != null) {
            handleForward(dto);
        } else {
            throw new IllegalArgumentException("Either replyToMessageId or forwardMessageId must be provided.");
        }
    }
    private void handleReply(ReplyForwardMessageDTO dto) {
        ChatMessage original = messageRepository.findById(dto.getReplyToMessageId())
                .orElseThrow(() -> new NotFoundException("Original message not found"));
        
        ChatMessage message = new ChatMessage();
        message.setSender(dto.getSender());
        message.setType(dto.getType()); 
        message.setTimestamp(LocalDateTime.now());
        message.setContent(dto.getContent());
        message.setReplyToMessage(original);
        message.setReplyPreviewContent(original.getContent());
        message.setClientId(dto.getClientId());
        
        
        if ("PRIVATE".equalsIgnoreCase(dto.getType())) {
            message.setReceiver(dto.getReceiver());
        } else {
            message.setGroupId(dto.getGroupId());
        }
        
        ChatMessage saved = messageRepository.save(message);
        ChatMessageResponse response = mapToResponse(saved);
        
        if ("PRIVATE".equalsIgnoreCase(dto.getType())) {
            messagingTemplate.convertAndSendToUser(dto.getReceiver(), "/queue/private", response);
            messagingTemplate.convertAndSendToUser(dto.getSender(), "/queue/private", response);
            
        } else {
            try {
                List<String> memberIds = teamService.getEmployeeIdsByTeamId(dto.getGroupId());
                for (String memberId : memberIds) {
                    if (!memberId.equals(dto.getSender())) { // skip sender if needed
                        log.info("Sending reply message to member {} in group {}", memberId, dto.getGroupId());
                        messagingTemplate.convertAndSendToUser(memberId, "/queue/private", response);
                    }
                }
                messagingTemplate.convertAndSendToUser(dto.getSender(), "/queue/private-ack", response);
                log.info("Sent reply acknowledgment to sender {} for group {}", dto.getSender(), dto.getGroupId());
                
            } catch (NotFoundException e) {
                log.error("Team not found for groupId: {}. Cannot send reply notifications.", dto.getGroupId());
            }
        }
        
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setSender(dto.getSender());
        notificationRequest.setMessage(dto.getContent());
        notificationRequest.setCategory(null);
        notificationRequest.setType("CHAT");
        notificationRequest.setSubject("Message Reply from "+ allEmployees.getEmployeeById(dto.getSender()).getEmployeeName());
        
        if ("PRIVATE".equalsIgnoreCase(dto.getType())) {
            notificationRequest.setReceiver(dto.getReceiver());
            notificationRequest.setLink("/chat/" + dto.getReceiver() + "/with?id=" + dto.getSender());
            try {
                notificationClient.send(notificationRequest);
                log.info("📩 Private reply notification sent to {}", dto.getReceiver());
            } catch (Exception e) {
                log.error("❌ Failed to send private reply notification: {}", e.getMessage());
            }
        } else {
            try {
                List<String> memberIds = teamService.getEmployeeIdsByTeamId(dto.getGroupId());
                for (String memberId : memberIds) {
                    if (!memberId.equals(dto.getSender())) {
                        notificationRequest.setReceiver(memberId);
                        notificationRequest.setLink("/chat/" + memberId + "/with?id=" + dto.getGroupId());
                        notificationClient.send(notificationRequest);
                        log.info("📩 Group reply notification sent to {}", memberId);
                    }
                }
            } catch (NotFoundException e) {
                log.error("Team not found for groupId: {}. Cannot send reply notifications.", dto.getGroupId());
            }
        }
        
        log.info("Message with ID: {} successfully replied to message with ID: {}", saved.getId(), original.getId());

    }
    
    private void handleForward(ReplyForwardMessageDTO dto) {
        ChatMessage original = messageRepository.findById(dto.getForwardMessageId())
                .orElseThrow(() -> new NotFoundException("Original message not found"));
        
        String trueOriginalSender = Boolean.TRUE.equals(original.getForwarded())
                ? original.getForwardedFrom()
                : original.getSender();
        for (ForwardTarget target : dto.getForwardTo()) {
            ChatMessage message = new ChatMessage();
            message.setSender(dto.getSender());
            message.setContent(original.getContent());
            message.setFileName(original.getFileName());
            message.setFileType(original.getFileType());
            message.setFileData(original.getFileData());
            message.setFileSize(original.getFileSize());
            message.setForwarded(true);
            message.setForwardedFrom(trueOriginalSender);
            message.setTimestamp(LocalDateTime.now());
            message.setDuration(original.getDuration());
            
            if (target.getReceiver() != null) {
                message.setType("PRIVATE");
                message.setReceiver(target.getReceiver());
            } else if (target.getGroupId() != null) {
                message.setType("TEAM");
                message.setGroupId(target.getGroupId());
            } else {
                throw new InvalidForwardTargetException("Forward target must have either receiver or groupId.");
            }
            ChatMessage saved = messageRepository.save(message);
            ChatMessageResponse response = mapToResponse(saved);
            
            if ("PRIVATE".equalsIgnoreCase(message.getType())) {
                messagingTemplate.convertAndSendToUser(target.getReceiver(), "/queue/private", response);
            } else {
                try {
                    List<String> memberIds = teamService.getEmployeeIdsByTeamId(target.getGroupId());
                    for(String memberId : memberIds) {
                        if(!memberId.equals(dto.getSender())){
                            log.info("Replying to a messgae by {} in the {} group ", memberId,dto.getGroupId());
                            messagingTemplate.convertAndSendToUser(memberId, "/queue/private", response);
                        }
                    }
                } catch (NotFoundException e) {
                    log.error("Team not found for groupId: {}. Cannot send forward notifications.", target.getGroupId());
                }
            }
            log.info("Message with ID: {} successfully forwarded from original message with ID: {}", saved.getId(), original.getId());
       
			}
            
        }

        
    
    private ChatMessageResponse mapToResponse(ChatMessage message) {
        ChatMessageResponse response = new ChatMessageResponse(
                message.getId(),
                message.getSender(),
                message.getReceiver(),
                message.getGroupId(),
                message.getContent(),
                message.getFileName(),
                message.getFileType(),
                message.getFileSize(),
                message.getType(),
                message.getTimestamp(),
                null,
                message.getClientId(),
                message.isEdited(),
                message.getDuration()
                );
 
        response.setForwarded(message.getForwarded());
        response.setForwardedFrom(message.getForwardedFrom());
        if (message.getReplyToMessage() != null) {
            ChatMessage original = message.getReplyToMessage();
            String originalMessageType = "text";
            if (original.getFileName() != null && original.getFileType() != null) {
                if (original.getFileType().startsWith("image/")) originalMessageType = "image";
                else if (original.getFileType().startsWith("audio/")) originalMessageType = "audio";
                else originalMessageType = "file";
            }
            ChatMessageResponse.ReplyInfo replyInfo = ChatMessageResponse.ReplyInfo.builder()
                .senderId(original.getSender())
                .content(message.getReplyPreviewContent())
                .originalMessageId(original.getId())
                .type(originalMessageType)
                .build();
            response.setReplyTo(replyInfo);
        }
        return response;
    }
}