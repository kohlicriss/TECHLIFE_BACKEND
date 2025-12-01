package com.app.chat_service.controller;

import com.app.chat_service.dto.ChatMessageRequest;
import com.app.chat_service.dto.ClearChatRequest;
import com.app.chat_service.dto.NotificationRequest;
import com.app.chat_service.dto.ReplyForwardMessageDTO;
import com.app.chat_service.dto.TypingStatusDTO;
import com.app.chat_service.feignclient.CustomFeignContext;
import com.app.chat_service.feignclient.NotificationClient;
import com.app.chat_service.handler.IlleagalArgumentsException;
import com.app.chat_service.handler.NotFoundException;
import com.app.chat_service.kakfa.KafkaMessageProcessorService;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.repo.ClearedChatRepository;
import com.app.chat_service.service.AllEmployees;
import com.app.chat_service.service.ChatForwardService;
import com.app.chat_service.service.ChatMessageOverviewService;
import com.app.chat_service.service.ChatMessageService;
import com.app.chat_service.service.ChatPresenceTracker;
import com.app.chat_service.service.ClearedChatService;
import com.app.chat_service.service.TeamService;
import com.app.chat_service.service.UpdateChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class WebSocketChatController {

   
    private final ChatPresenceTracker chatTracker;
    private final ChatMessageService chatMessageService;
    private final ChatForwardService chatForwardService;
    private final ChatMessageRepository chatMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UpdateChatMessageService updateChatMessageService;
    private final ClearedChatRepository clearedChatRepository;
    private final ClearedChatService clearedChatService;
    private final KafkaMessageProcessorService messageProcessor;
    private final ChatMessageOverviewService chatMessageOverviewService;
    private final TeamService teamService;
    private final CustomFeignContext customFeignContext;
    private final NotificationClient notificationClient;
    private final AllEmployees allEmployees; 
    
    
    public WebSocketChatController(
                                   ChatPresenceTracker chatTracker,
                                   ChatMessageService chatMessageService,
                                   ChatForwardService chatForwardService,
                                   ChatMessageRepository chatMessageRepository,
                                   SimpMessagingTemplate messagingTemplate,
                                   UpdateChatMessageService updateChatMessageService,
                                   ClearedChatRepository clearedChatRepository,
                                   ClearedChatService clearedChatService,
                                   KafkaMessageProcessorService messageProcessor,
                                   ChatMessageOverviewService chatMessageOverviewService,
                                   TeamService teamService,
                                   CustomFeignContext customFeignContext,
                                   NotificationClient notificationClient,
                                   AllEmployees allEmployees){
        this.chatTracker = chatTracker;
        this.chatMessageService = chatMessageService;
        this.chatForwardService = chatForwardService;
        this.chatMessageRepository = chatMessageRepository;
        this.messagingTemplate = messagingTemplate;
        this.updateChatMessageService = updateChatMessageService;
        this.clearedChatRepository = clearedChatRepository;
        this.clearedChatService = clearedChatService;
        this.messageProcessor=messageProcessor;
        this.chatMessageOverviewService=chatMessageOverviewService;
        this.teamService=teamService;
        this.customFeignContext=customFeignContext;
        this.notificationClient=notificationClient;
        this.allEmployees = allEmployees;
        }

    // Mark chat as opened, start read process
    @MessageMapping("/presence/open/{target}")
    public void openChat(@DestinationVariable String target, Principal principal) {
        String user = principal.getName();
        customFeignContext.setUser(user);
        log.info("user {} opened chat with target {}", user,target);
        chatTracker.openChat(user, target);

        if (target.toUpperCase().startsWith("TEAM")) {
            chatMessageService.markGroupMessagesAsRead(user, target);
        } else {
            chatMessageService.markMessagesAsRead(user, target);
        }
        // Broadcasts are now inside service after DB commit (fixes unread count bug)
    }

    // Mark chat as closed
    @MessageMapping("/presence/close/{target}")
    public void closeChat(@DestinationVariable String target, Principal principal) {
        String user = principal.getName();
        log.info("User {} closed chat with target {} ",user,target);
        chatTracker.closeChat(user, target);

        chatMessageService.broadcastChatOverview(user);
        chatMessageService.broadcastChatOverview(target);
    }

    // Send chat message (with ACK)
    @MessageMapping("/send")
    @Transactional
    public void handleMessage(@Payload ChatMessageRequest request, SimpMessageHeaderAccessor headerAccessor) {

        String token = (String) headerAccessor.getSessionAttributes().get("Authorization");

        if (token != null) {
            customFeignContext.setToken(token); // Store it in ThreadLocal for Feign interceptor
            log.info("Forwarding token for Feign client: {}", token);
        } else {
            log.warn("No token found in WebSocket session.");
        }

        log.info("➡️ Message received in backend: {}", request);

        
        ChatMessage message = new ChatMessage();
        message.setSender(request.getSender());
        message.setReceiver(request.getReceiver());
        message.setContent(request.getContent());
        message.setType(request.getType());
        message.setGroupId(request.getGroupId());
        message.setTimestamp(LocalDateTime.now());
        message.setClientId(request.getClientId());

        ChatMessage savedMessage = chatMessageRepository.save(message);
        log.info("✅ Message saved to DB with ID: {}", savedMessage.getId());

       
        NotificationRequest notificationRequest = new NotificationRequest();
        notificationRequest.setReceiver(request.getReceiver());
        notificationRequest.setMessage(request.getContent());
        notificationRequest.setSender(request.getSender());
        notificationRequest.setCategory(null);
        notificationRequest.setKind(request.getKind());
        notificationRequest.setSubject("New message received from "+allEmployees.getEmployeeById(request.getSender()).getEmployeeName());
        notificationRequest.setType("CHAT");
        
        if (request.getGroupId() != null && !request.getGroupId().isBlank()) {
        	notificationRequest.setLink("/chat/" + request.getReceiver() + "/with?id=" + request.getGroupId());
        	
        } else {
        	notificationRequest.setLink("/chat/" + request.getReceiver() + "/with?id=" + request.getSender());
        }
        
        if (request.getGroupId() != null && !request.getGroupId().isBlank()) {
            // Get group members from your team/employee microservice
            List<String> groupMembers = teamService.getEmployeeIdsByTeamId(request.getGroupId());

            for (String memberId : groupMembers) {
                if (!memberId.equals(request.getSender())) {
                    notificationRequest.setReceiver(memberId);
                    try {
                        notificationClient.send(notificationRequest);
                        log.info("📩 Group notification sent to {}", memberId);
                    } catch (Exception e) {
                        log.error("❌ Failed to send notification to {}: {}", memberId, e.getMessage());
                    }
                }
            }
        } else {
            // Private chat
            try {
                notificationClient.send(notificationRequest);
                log.info("📩 Private notification sent to {}", request.getReceiver());
            } catch (Exception e) {
                log.error("❌ Failed to send private notification: {}", e.getMessage());
            }
        }
 
        messageProcessor.processChatMessage(savedMessage);
        log.info("Message sent to message processor: {}", savedMessage);
    }


    // Edit existing message
    @MessageMapping("/edit")
    public void handleEditMessage(@Payload ChatMessageRequest updatedRequest, SimpMessageHeaderAccessor headerAccessor) {
        log.info("✏️ Edit request received: {}", updatedRequest);
        
        String token = (String) headerAccessor.getSessionAttributes().get("Authorization");
        
	 		if (token != null) {
	 			customFeignContext.setToken(token);
	 			log.info("Forwarding token for Feign client: {}", token);
	 		} else {
	 			log.warn("No token found in WebSocket session.");
	 		}

        if (updatedRequest.getMessageId() == null) {
            log.warn("❌ Edit request missing messageId");
            return;
        }

        try {
            updateChatMessageService.updateChatMessage(
                    updatedRequest.getMessageId(),
                    updatedRequest
            );
            log.info("✅ Message edited successfully with id {} and content {}",
                    updatedRequest.getMessageId(), updatedRequest.getContent());
        } catch (NotFoundException | IlleagalArgumentsException e) {
            log.warn("❌ Edit failed: {}", e.getMessage());
            
            
             if (updatedRequest.getSender() != null) {
                 messagingTemplate.convertAndSendToUser(updatedRequest.getSender(), "/queue/errors", Map.of("error", e.getMessage()));
             }
        }
    }

    // Reply to message
    @MessageMapping("/reply")
    public void handleReply(ReplyForwardMessageDTO dto, SimpMessageHeaderAccessor headerAccessor) {
    	log.info("Reply request received: {} ",dto);
    	
    	String token = (String) headerAccessor.getSessionAttributes().get("Authorization");
        if (token != null) {
            customFeignContext.setToken(token);
            log.info("Forwarding token for Feign client: {}", token);
        } else {
            log.warn("No token found in WebSocket session.");
        }
        
        chatForwardService.handleReplyOrForward(dto);
    }

    // Forward message
    @MessageMapping("/forward")
    public void handleForward(ReplyForwardMessageDTO dto, SimpMessageHeaderAccessor headerAccessor) {
    	log.info("Forward request received: {}", dto);
    	
    	String token = (String) headerAccessor.getSessionAttributes().get("Authorization");
        if (token != null) {
            customFeignContext.setToken(token);
            log.info("Forwarding token for Feign client: {}", token);
        } else {
            log.warn("No token found in WebSocket session.");
        }
    	chatForwardService.handleReplyOrForward(dto);
    }

    /**
     * Endpoint for clearing a chat
     * @param dto - contains userId and chatId
     */
    @MessageMapping("/clear")
    public void clearChat(@Payload ClearChatRequest dto, Principal principal) {
    	
        String authenticatedUserId = principal.getName();
        log.info("Clear chat request received from user: {} for chat: {} ", authenticatedUserId, dto.getChatId());
        clearedChatService.clearChat(authenticatedUserId, dto.getChatId());

        messagingTemplate.convertAndSendToUser(
                authenticatedUserId,
                "/queue/clearchat",
                Map.of(
                        "message", "Chat cleared successfully for chatId: " + dto.getChatId(),
                        "chatId", dto.getChatId()
                )
        );
    }      
    
    /**
     * Handles typing status updates from clients.
     */
    @MessageMapping("/typing")
    public void handleTypingStatus(@Payload TypingStatusDTO dto, SimpMessageHeaderAccessor headerAccessor) {
        log.info("Typing status received: {}", dto);
 		String token = (String) headerAccessor.getSessionAttributes().get("Authorization");
 
	 		if (token != null) {
	 			customFeignContext.setToken(token);
	 			log.info("Forwarding token for Feign client: {}", token);
	 		} else {
	 			log.warn("No token found in WebSocket session.");
	 		}
	 		
        if ("PRIVATE".equalsIgnoreCase(dto.getType())) {
            
            messagingTemplate.convertAndSendToUser(dto.getReceiverId(),"/queue/typing-status",dto );
                    
        } else if ("TEAM".equalsIgnoreCase(dto.getType()) || "DEPARTMENT".equalsIgnoreCase(dto.getType())) {
        	
        	List<String> membersIds = teamService.getEmployeeIdsByTeamId(dto.getGroupId());
        	for (String member : membersIds) {
        		if (!member.equals(dto.getSenderId())) {	
        		log.info("for group typing status is sending to  /topic/typing-status/{}", dto.getGroupId());
        		messagingTemplate.convertAndSendToUser(member, "queue/typing-status", dto);
        		}
        	}
        		
        }
        	
        	
    }

}