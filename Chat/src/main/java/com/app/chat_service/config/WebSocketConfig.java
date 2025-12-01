package com.app.chat_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

import com.app.chat_service.feignclient.WebSocketAuthInterceptor;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	
    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    public WebSocketConfig(WebSocketAuthInterceptor webSocketAuthInterceptor) {
        this.webSocketAuthInterceptor = webSocketAuthInterceptor;
    }	

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable in-memory broker for topics and queues
        registry.enableSimpleBroker("/topic", "/queue");

        // Prefix for messages sent from client to server (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix required for convertAndSendToUser()
        registry.setUserDestinationPrefix("/user");

        log.info("WebSocket message broker configured: prefixes /app, /user, /topic, /queue");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint for client connections (SockJS optional)
        registry.addEndpoint("/api/chat")
                .setHandshakeHandler(new CustomHandshakeHandler())
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOriginPatterns("*"); // ✅ Recommended for Spring Boot 3+
        log.info("WebSocket STOMP endpoint [/api/chat] registered");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(10 * 1024 * 1024); // 10 MB
    }
}
