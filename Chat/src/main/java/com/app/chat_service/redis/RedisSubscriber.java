package com.app.chat_service.redis;

import com.app.chat_service.dto.ChatMessageResponse;
import com.app.chat_service.dto.MessageStatusUpdateDTO;
import com.app.chat_service.feignclient.CustomFeignContext;
import com.app.chat_service.model.ChatMessage;
import com.app.chat_service.model.MessageReadStatus;
import com.app.chat_service.repo.ChatMessageRepository;
import com.app.chat_service.repo.MessageReadStatusRepository;
import com.app.chat_service.service.ChatPresenceTracker;
import com.app.chat_service.service.OnlineUserService;
import com.app.chat_service.service.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.AbstractMessage;

import io.netty.handler.codec.MessageAggregationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** The RedisSubscriber will handle the actual delivery to WebSocket clients. **/

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final ChatPresenceTracker chatTracker;
    private final TeamService teamService;
    private final OnlineUserService onlineUserService;
    private final ChatMessageRepository chatMessageRepository;
    private final CustomFeignContext customFeignContext;
    private final MessageReadStatusRepository readStatusRepo;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            ChatMessageResponse chatMessage = objectMapper.readValue(body, ChatMessageResponse.class);
            log.info("Received message {} from Redis. {}", chatMessage.getId(), chatMessage);

            if ("PRIVATE".equalsIgnoreCase(chatMessage.getType())) {
                handlePrivateMessage(chatMessage);
            } else if ("TEAM".equalsIgnoreCase(chatMessage.getType())) {
                handleTeamMessage(chatMessage);
            }

        } catch (Exception e) {
            log.error("❌ Error processing Redis message", e);
        }
    }

    private void handlePrivateMessage(ChatMessageResponse chatMessage) {
        String targetUser = chatMessage.getReceiver();
        String senderUser = chatMessage.getSender();

        if (senderUser != null) {
            messagingTemplate.convertAndSendToUser(senderUser, "/queue/private-ack", chatMessage);
        }

        if (targetUser == null) {
            return;
        }

        boolean isWindowOpen = chatTracker.isChatWindowOpen(targetUser, senderUser);

        if (isWindowOpen) {
            
            chatMessageRepository.findById(chatMessage.getId()).ifPresent(msg -> {
                if (!msg.isRead()) {
                    msg.setRead(true);
                    chatMessageRepository.save(msg);
                    log.info("Marked new message {} as read from RedisSubscriber", msg.getId());
                }
            });
           
            chatMessage.setSeen(true);
      
            messagingTemplate.convertAndSendToUser(targetUser, "/queue/private", chatMessage);

            
            MessageStatusUpdateDTO statusUpdate = MessageStatusUpdateDTO.builder()
                    .type("STATUS_UPDATE")
                    .status("SEEN")
                    .chatId(targetUser) 
                    .messageIds(List.of(chatMessage.getId()))
                    .build();
            messagingTemplate.convertAndSendToUser(senderUser, "/queue/private", statusUpdate);
            log.info("Sent SEEN status for new message {} to sender {}", chatMessage.getId(), senderUser);

        } else {
            
            messagingTemplate.convertAndSendToUser(targetUser, "/queue/private", chatMessage);

            
            if (onlineUserService.isOnline(targetUser)) {
                MessageStatusUpdateDTO statusUpdate = MessageStatusUpdateDTO.builder()
                        .type("STATUS_UPDATE")
                        .status("DELIVERED")
                        .chatId(targetUser)
                        .messageIds(List.of(chatMessage.getId()))
                        .build();
                messagingTemplate.convertAndSendToUser(senderUser, "/queue/private", statusUpdate);
                log.info("Sent DELIVERED status for message {} to sender {}", chatMessage.getId(), senderUser);
            }
        }
    }

    private void handleTeamMessage(ChatMessageResponse chatMessage) {
        String teamId = chatMessage.getGroupId();
        String senderId = chatMessage.getSender();
        ChatMessageResponse actMessage = chatMessage;
        String targetUser = chatMessage.getReceiver();
        messagingTemplate.convertAndSendToUser(chatMessage.getReceiver(), "/queue/private-ack", actMessage);
        log.info("message recived from {} from team {} to user {}", actMessage.getSender(), actMessage.getGroupId(), chatMessage.getReceiver());
        
        
        if (targetUser.equals(senderId)) {
            log.info("Sender {} received ack for their own message. No delivery needed.", senderId);
            return;
        }

       
        boolean isWindowOpen = chatTracker.isChatWindowOpen(targetUser, teamId);

        if (isWindowOpen) {
           
            log.info("User {} HAS window open for team {}. Marking as read.", targetUser, teamId);
            
            chatMessageRepository.findById(chatMessage.getId()).ifPresent(msg -> {
                MessageReadStatus readStatus = MessageReadStatus.builder()
                        .chatMessage(msg)
                        .userId(targetUser)
                        .readAt(LocalDateTime.now())
                        .build();
                readStatusRepo.save(readStatus);
                log.info("Saved MessageReadStatus for user {} and message {}", targetUser, msg.getId());
            });
           
            chatMessage.setSeen(true);

        } else {
            
            log.info("User {} has window CLOSED for team {}. Marking as unread.", targetUser, teamId);
            chatMessage.setSeen(false);
        }

        
        messagingTemplate.convertAndSendToUser(targetUser, "/queue/private", chatMessage);
        log.info("Delivered TEAM message {} to targetUser {}", chatMessage.getId(), targetUser);
    }
        
        
}
    






     




     


     

     
