package com.example.employee.Service;


import com.example.employee.WebSocket.TicketWebSocketHandler;
import com.example.employee.client.NotificationClient;
import com.example.employee.dto.TicketDTO;
import com.example.employee.dto.TicketReplyDTO;
import com.example.employee.entity.NotificationRequest;
import com.example.employee.redis.RedisMessagePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncTicketHelper {

    private final RedisMessagePublisher redisPublisher;
    private final NotificationClient notificationClient;
    private final TicketWebSocketHandler ticketWebSocketHandler;


    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Async("taskExecutor")
    public void publishTicketToRedisAsync(TicketDTO ticketDTO) {
        try {
            String json = objectMapper.writeValueAsString(ticketDTO);
            redisPublisher.publish(json);
            log.info("[Async] Published ticket {} to Redis", ticketDTO.getTicketId());
        } catch (Exception e) {
            log.error("[Async] Failed to publish ticket {} to Redis", ticketDTO.getTicketId(), e);
        }
    }

    @Async
    public void sendNotificationAsync(NotificationRequest notification) {
        try {
            log.info("Sending async notification to: {}", notification.getReceiver());
            notificationClient.send(notification);
            log.info("Async notification sent successfully to: {}", notification.getReceiver());
        } catch (Exception e) {
            log.error("Failed to send async notification to {}: {}",
                    notification.getReceiver(), e.getMessage(), e);

        }
    }

    @Async("taskExecutor")
    public void broadcastReplyAsync(TicketReplyDTO replyDTO) {
        try {
            ticketWebSocketHandler.broadcastToTicket(replyDTO);
            log.info("[Async] Broadcasted reply for ticket {}", replyDTO.getTicketId());
        } catch (Exception e) {
            log.error("[Async] Failed to broadcast reply for {}", replyDTO.getTicketId(), e);
        }
    }
}
