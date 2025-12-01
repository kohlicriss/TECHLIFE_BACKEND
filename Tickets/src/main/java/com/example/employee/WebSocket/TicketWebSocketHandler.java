package com.example.employee.WebSocket;

import com.example.employee.dto.TicketReplyDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TicketWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, List<WebSocketSession>> sessionsByTicket = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String ticketId = getTicketId(session);
        sessionsByTicket.computeIfAbsent(ticketId, k -> new ArrayList<>()).add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String ticketId = getTicketId(session);
      List<WebSocketSession> sessions = sessionsByTicket.get(ticketId);
if (sessions != null) {
    sessions.remove(session);
    if (sessions.isEmpty()) {
        sessionsByTicket.remove(ticketId); 
    }
}

    }

  private String getTicketId(WebSocketSession session) {
    String query = session.getUri().getQuery(); 
    if (query == null) return "unknown";

    for (String param : query.split("&")) {
        String[] pair = param.split("=");
        if (pair.length == 2 && pair[0].equals("ticketId")) {
            return pair[1];
        }
    }
    return "unknown";
}


    public void broadcastToTicket(TicketReplyDTO reply) {
        try {
            String ticketId = reply.getTicketId();
            String message = objectMapper.writeValueAsString(reply);

            if (sessionsByTicket.containsKey(ticketId)) {
                for (WebSocketSession session : sessionsByTicket.get(ticketId)) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(message));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
