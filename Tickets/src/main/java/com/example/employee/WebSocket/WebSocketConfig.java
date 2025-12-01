package com.example.employee.WebSocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final UnifiedTicketWebSocketHandler unifiedTicketWebSocketHandler;

    public WebSocketConfig(UnifiedTicketWebSocketHandler unifiedTicketWebSocketHandler) {
        this.unifiedTicketWebSocketHandler = unifiedTicketWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(unifiedTicketWebSocketHandler, "/api/ticket/ws")
                .setAllowedOriginPatterns("*");
                
    }
}