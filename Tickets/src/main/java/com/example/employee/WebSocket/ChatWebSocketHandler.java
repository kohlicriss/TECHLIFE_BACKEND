package com.example.employee.WebSocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>> sessionsByTicket = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String query = session.getUri().getQuery(); // e.g., "ticketId=123&userId=EMP001"
        if (query == null) return;

        String ticketId = null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals("ticketId")) {
                ticketId = pair[1];
                break;
            }
        }

        if (ticketId == null) ticketId = "unknown";

        sessionsByTicket.computeIfAbsent(ticketId, k -> new CopyOnWriteArrayList<>()).add(session);

        System.out.println("✅ Session connected for ticket: " + ticketId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);

        String ticketId = chatMessage.getTicketId();
        if (ticketId == null) return;

        
        if (sessionsByTicket.containsKey(ticketId)) {
            for (WebSocketSession s : sessionsByTicket.get(ticketId)) {
                if (s.isOpen()) {
                    s.sendMessage(new TextMessage(objectMapper.writeValueAsString(chatMessage)));
                }
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        
        sessionsByTicket.values().forEach(list -> list.remove(session));

       
        sessionsByTicket.entrySet().removeIf(entry -> entry.getValue().isEmpty());

        System.out.println("❌ Session disconnected: " + session.getId());
    }

   
    public static class ChatMessage {
        private String ticketId;
        private String senderId;
        private String receiverId; 
        private String message;

        // Getters and setters
        public String getTicketId() { return ticketId; }
        public void setTicketId(String ticketId) { this.ticketId = ticketId; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String senderId) { this.senderId = senderId; }
        public String getReceiverId() { return receiverId; }
        public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
