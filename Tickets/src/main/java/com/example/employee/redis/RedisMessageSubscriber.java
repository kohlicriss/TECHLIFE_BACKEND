package com.example.employee.redis;

import com.example.employee.WebSocket.TicketWebSocketHandler;
import com.example.employee.WebSocket.UnifiedTicketWebSocketHandler;
import com.example.employee.dto.TicketDTO;
import com.example.employee.dto.TicketReplyDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

@Service
public class RedisMessageSubscriber implements MessageListener {

    private final ObjectMapper objectMapper;
    private final UnifiedTicketWebSocketHandler ticketWebSocketHandler;

    public RedisMessageSubscriber(ObjectMapper objectMapper,
                                  UnifiedTicketWebSocketHandler ticketWebSocketHandler) {
        this.objectMapper = objectMapper;
        this.ticketWebSocketHandler = ticketWebSocketHandler;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String raw = new String(message.getBody());
        System.out.println("Raw Redis message: " + raw);

        try {
            String json = raw.startsWith("\"")
                    ? raw.substring(1, raw.length() - 1).replace("\\\"", "\"")
                    : raw;

          
            if (json.contains("\"description\"") || json.contains("\"title\"")) {
                
                TicketDTO ticketDTO = objectMapper.readValue(json, TicketDTO.class);
                System.out.println("Received TicketDTO: " + ticketDTO);
                
            } else {
                
                TicketReplyDTO replyDTO = objectMapper.readValue(json, TicketReplyDTO.class);
                System.out.println("Received TicketReplyDTO: " + replyDTO);
                ticketWebSocketHandler.broadcastToTicket(replyDTO);
            }

        } catch (Exception e) {
            System.err.println("Redis -> DTO conversion failed: " + raw);
            e.printStackTrace();
        }
    }
}
