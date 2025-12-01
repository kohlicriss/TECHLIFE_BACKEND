package com.example.employee.WebSocket;

import com.example.employee.dto.TicketReplyDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class UnifiedTicketWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, CopyOnWriteArrayList<WebSocketSession>> sessionsByTicket = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String ticketId = extractTicketId(session);
        String token = extractToken(session);

        System.out.println("🎯 NEW WEBSOCKET CONNECTION =======");
        System.out.println("🔗 URI: " + session.getUri());
        System.out.println("📋 Ticket ID: " + ticketId);
        System.out.println("🔑 Token: " + (token != null ? "Present" : "Missing"));

        if (ticketId == null || ticketId.trim().isEmpty()) {
            System.out.println("❌ MISSING TICKET ID - CLOSING CONNECTION");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing ticketId"));
            return;
        }

        if (token == null || token.trim().isEmpty()) {
            System.out.println("❌ MISSING TOKEN - CLOSING CONNECTION");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Missing token"));
            return;
        }

        // Normalize ticket ID
        String normalizedTicketId = ticketId.toUpperCase().trim();
        
        // Add session to tracking
        sessionsByTicket.computeIfAbsent(normalizedTicketId, k -> new CopyOnWriteArrayList<>()).add(session);
        
        System.out.println("✅ CONNECTED: Session " + session.getId() + " → Ticket " + normalizedTicketId);
        System.out.println("📊 Active sessions for " + normalizedTicketId + ": " + sessionsByTicket.get(normalizedTicketId).size());
        System.out.println("🎯 ACTIVE TICKETS: " + sessionsByTicket.keySet());
    }

    // FIXED BROADCAST METHOD - This is called from your service
    public void broadcastToTicket(TicketReplyDTO reply) {
        try {
            if (reply == null || reply.getTicketId() == null) {
                System.err.println("❌ CANNOT BROADCAST: Reply or ticketId is null");
                return;
            }

            String ticketId = reply.getTicketId().toUpperCase().trim();
            String payload = objectMapper.writeValueAsString(reply);
            
            System.out.println("📢 BROADCASTING to " + ticketId + ": " + payload);
            
            // Broadcast immediately
            broadcastToTicket(ticketId, payload);
            
        } catch (Exception e) {
            System.err.println("❌ BROADCAST ERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void broadcastToTicket(String ticketId, String payload) {
        String normalizedTicketId = ticketId.toUpperCase().trim();
        CopyOnWriteArrayList<WebSocketSession> sessions = sessionsByTicket.get(normalizedTicketId);

        System.out.println(" Checking sessions for: " + normalizedTicketId);
        System.out.println(" Available tickets: " + sessionsByTicket.keySet());

        if (sessions == null || sessions.isEmpty()) {
            System.out.println(" NO ACTIVE SESSIONS for ticket: " + normalizedTicketId);
            return;
        }

        // Clean closed sessions
        sessions.removeIf(session -> !session.isOpen());
        
        if (sessions.isEmpty()) {
            sessionsByTicket.remove(normalizedTicketId);
            System.out.println(" Removed empty session list for: " + normalizedTicketId);
            return;
        }

        System.out.println("📤 Sending to " + sessions.size() + " session(s)");
        
        // Broadcast to all sessions
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(payload));
                    }
                    System.out.println("✅ Sent to session: " + session.getId());
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to send to session " + session.getId() + ": " + e.getMessage());
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("📩 Received message: " + message.getPayload());
        // Echo back for testing
        session.sendMessage(new TextMessage("{\"type\":\"echo\", \"message\":\"Received: " + message.getPayload() + "\"}"));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String ticketId = extractTicketId(session);
        if (ticketId != null) {
            String normalizedTicketId = ticketId.toUpperCase().trim();
            CopyOnWriteArrayList<WebSocketSession> sessions = sessionsByTicket.get(normalizedTicketId);
            if (sessions != null) {
                sessions.remove(session);
                System.out.println("❌ REMOVED session from " + normalizedTicketId);
                if (sessions.isEmpty()) {
                    sessionsByTicket.remove(normalizedTicketId);
                    System.out.println("🧹 CLEANED UP ticket: " + normalizedTicketId);
                }
            }
        }
        System.out.println("🔌 DISCONNECTED: " + session.getId());
    }

    private String extractTicketId(WebSocketSession session) {
        return extractParameter(session, "ticketId");
    }

    private String extractToken(WebSocketSession session) {
        return extractParameter(session, "token");
    }

    private String extractParameter(WebSocketSession session, String paramName) {
        try {
            if (session.getUri() == null || session.getUri().getQuery() == null) {
                return null;
            }
            
            String query = session.getUri().getQuery();
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2 && pair[0].equals(paramName)) {
                    return pair[1];
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("❌ Error extracting parameter: " + e.getMessage());
            return null;
        }
    }

    // Debug method
    public void printActiveSessions() {
        System.out.println("=== ACTIVE SESSIONS ===");
        sessionsByTicket.forEach((ticket, sessions) -> {
            System.out.println("Ticket: " + ticket + " | Sessions: " + sessions.size());
        });
        System.out.println("=======================");
    }
}