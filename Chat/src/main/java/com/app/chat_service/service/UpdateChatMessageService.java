package com.app.chat_service.service;
 
import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.handler.IlleagalArgumentsException;
import com.app.chat_service.handler.NotFoundException;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import jakarta.transaction.Transactional;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
 
@Slf4j
@Service
public class UpdateChatMessageService {
 
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TeamService teamService;
 
    public UpdateChatMessageService(ChatMessageRepository chatMessageRepository,
                                  SimpMessagingTemplate messagingTemplate,
                                  TeamService teamService) {
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.teamService = teamService;
    }
 
    @Transactional
    public void updateChatMessage(Long messageId, ChatMessageRequest updatedRequest) {
    	log.info("Edit message request received to backend with message id {}, and user {}:", updatedRequest.getMessageId(), updatedRequest.getSender());;
    	
    	ChatMessage message = chatMessageRepository.findById(messageId)
        		.orElseThrow(() -> new NotFoundException("Message not found with Id:"+messageId));
 
        if (updatedRequest == null || !StringUtils.hasText(updatedRequest.getSender())) {
        	throw new IlleagalArgumentsException("Sender information is required for validation.");
        }
 
        if (!updatedRequest.getSender().equals(message.getSender())) {
        	throw new IlleagalArgumentsException("You are not Authorized to edit");
        }
        
        if (StringUtils.hasText(updatedRequest.getContent())) {
            message.setContent(updatedRequest.getContent());
            message.setEdited(true); 
        } else {
        	log.info("Updated content cannot ne null");
            throw new IlleagalArgumentsException("Updated content cannot be null or empty");
        }
        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("Edited Message saved to db {}:", savedMessage);
        ;
        // Prepare the response DTO with the isEdited flag
        ChatMessageResponse response = new ChatMessageResponse(
                savedMessage.getId(),
                savedMessage.getSender(),
                savedMessage.getReceiver(),
                savedMessage.getGroupId(),
                savedMessage.getContent(),
                savedMessage.getFileName(),
                savedMessage.getFileType(),
                savedMessage.getFileSize(),
                savedMessage.getType(),
                savedMessage.getTimestamp(),
                null, 
                savedMessage.getClientId(),
                									
                savedMessage.isEdited(),		
                savedMessage.getDuration()
        
        	);
        
 
        String type = savedMessage.getType();
        if (type != null) {
            String destination;
            if ("PRIVATE".equals(type)) {
                if (savedMessage.getReceiver() != null) {
                    destination = "/user/" + savedMessage.getReceiver() + "/queue/private";
                    messagingTemplate.convertAndSend(destination, response);
                }
                // Also send to sender's queue to update their other sessions
                if(savedMessage.getSender() != null) {
                    destination = "/user/" + savedMessage.getSender() + "/queue/private";
                    messagingTemplate.convertAndSend(destination, response);
                }
 
            } else if ("TEAM".equals(type) || "DEPARTMENT".equals(type)) {
                if (savedMessage.getGroupId() != null) {
                	
                	List<String> memberIds = teamService.getEmployeeIdsByTeamId(savedMessage.getGroupId());
                	
                	for(String memberId : memberIds ) {
                		if(!memberId.equals(savedMessage.getSender())){
                			log.info("Sending edited message to member {} in group {}", memberId, savedMessage.getGroupId());
                			messagingTemplate.convertAndSendToUser(memberId,"/queue/private" , response);
                		}
                	}
                }
            } else {
                System.out.println("Warning: Unknown message type for WebSocket broadcast: " + type);
            }
        }
    }
}