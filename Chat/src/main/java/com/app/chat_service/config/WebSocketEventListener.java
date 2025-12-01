package com.app.chat_service.config;
 
import com.app.chat_service.feignclient.CustomFeignContext;
import com.app.chat_service.service.OnlineUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
 
import java.util.Map;
 
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {
 
    private final OnlineUserService onlineUserService;
    // SimpMessagingTemplate is needed to send messages to WebSocket topics
    private final SimpMessagingTemplate messagingTemplate;
    private final CustomFeignContext customFeignContext;
 
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        String userId = event.getUser() != null ? event.getUser().getName() : null;
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
 
        if (userId != null) {
            onlineUserService.addUser(userId, sessionId);
            log.info("User connected: {} (sessionId={})", userId, sessionId);
            customFeignContext.setUser(userId);
            
            
            Map<String, Object> statusPayload = Map.of(
                    "userId", userId,
                    "isOnline", true
            );
           
            messagingTemplate.convertAndSend("/topic/presence", statusPayload);
          
 
        } else {
            log.warn("Connection without employeeId in Principal");
        }
    }
 
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String employeeId = event.getUser() != null ? event.getUser().getName() : null;
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
 
        if (employeeId != null) {
            onlineUserService.removeUser(employeeId, sessionId);
            log.info("User disconnected: {} (sessionId={})", employeeId, sessionId);
 
           
            Map<String, Object> statusPayload = Map.of(
                    "userId", employeeId,
                    "isOnline", false
            );
            
            messagingTemplate.convertAndSend("/topic/presence", statusPayload);
            
 
        } else {
            log.warn("Disconnection without employeeId in Principal");
        }
    }
}