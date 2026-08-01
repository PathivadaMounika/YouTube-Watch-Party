package com.watchparty.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Clients connect here (via SockJS fallback) to open the WebSocket.
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:5173")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Messages the server pushes to clients go out on "/topic/..."
        // e.g. /topic/room/AB3XQ9
        registry.enableSimpleBroker("/topic");

        // Messages clients send to the server are prefixed with "/app"
        // and routed to @MessageMapping methods.
        registry.setApplicationDestinationPrefixes("/app");
    }
}
