package com.app.chat_service.kakfa;

import com.app.chat_service.dto.ChatMessageResponse;
//import com.app.chat_service.kakfa.ChatKafkaConsumer;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.MessageReadStatus;
import com.app.chat_service.redis.RedisPublisherService;
import com.app.chat_service.repo.MessageReadStatusRepository;
import com.app.chat_service.service.ChatMessageService;
import com.app.chat_service.service.ChatPresenceTracker;
import com.app.chat_service.service.TeamService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j

public class KafkaMessageProcessorService {

    private final RedisPublisherService redisPublisher;
    private final ChatMessageService chatMessageService;
    private final ChatPresenceTracker presenceTracker;
    private final MessageReadStatusRepository readStatusRepo;
    private final TeamService teamService;
    private final SimpMessagingTemplate messagingTemplate;


    
//    @Async("asyncTaskExecutor") 
    @Transactional
    public void processChatMessage(ChatMessage incomingMessage) {
        log.info("Received message to processer ID: {}", incomingMessage);
        

        boolean isDeleted = "This message was deleted".equalsIgnoreCase(incomingMessage.getContent());
        boolean isPrivateRead = false;

        if ("PRIVATE".equalsIgnoreCase(incomingMessage.getType())) {
            String receiverId = incomingMessage.getReceiver();
            String senderId = incomingMessage.getSender();
            if (receiverId != null && presenceTracker.isChatWindowOpen(receiverId, senderId)) {
                isPrivateRead = true;
            }
        }

        ChatMessageResponse response = new ChatMessageResponse(
                incomingMessage.getId(), incomingMessage.getSender(), incomingMessage.getReceiver(),
                incomingMessage.getGroupId(), incomingMessage.getContent(), incomingMessage.getFileName(),
                incomingMessage.getFileType(), incomingMessage.getFileSize(), incomingMessage.getType(),
                incomingMessage.getTimestamp(), null, incomingMessage.getClientId(), incomingMessage.getDuration()
        );

        if (incomingMessage.getFileName() != null) {
            response.setFileUrl("/api/chat/file/" + incomingMessage.getId());
        }

        response.setSeen(isPrivateRead);
        if (isDeleted) response.setIsDeleted(true);
        
        if ("PRIVATE".equalsIgnoreCase(incomingMessage.getType())) {
        	redisPublisher.publish(response);
        } 
        else {
	        List<String> memberIds = teamService.getEmployeeIdsByTeamId(incomingMessage.getGroupId());
	        
	        for(String member: memberIds) {
	        	response.setReceiver(member);
	        	log.info("Setting members at the processer{} {}",member, response);
	        	redisPublisher.publish(response);
	            log.info("🚀 Message sent from async processor to Redis pub. ID: {}", incomingMessage.getId());
	        }
        }

//        chatMessageService.broadcastChatOverview(incomingMessage.getSender());
//      
//        if ("PRIVATE".equalsIgnoreCase(incomingMessage.getType()) && incomingMessage.getReceiver() != null) {
//            chatMessageService.broadcastChatOverview(incomingMessage.getReceiver());
//        } else if ("TEAM".equalsIgnoreCase(incomingMessage.getType()) && incomingMessage.getGroupId() != null) {
//            chatMessageService.broadcastGroupChatOverview(incomingMessage.getGroupId());
//        }
//        log.info("Async processing finished for message ID: {}", incomingMessage.getId());
    }
}