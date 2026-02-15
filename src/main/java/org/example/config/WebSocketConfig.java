package org.example.config;

import org.example.handler.ChatWebSocketHandler;
import org.example.handler.OrderWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Bean
    public OrderWebSocketHandler orderWebSocketHandler() {
        return new OrderWebSocketHandler();
    }
    
    @Bean
    public ChatWebSocketHandler chatWebSocketHandler() {
        return new ChatWebSocketHandler();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(orderWebSocketHandler(), "/ws/orders").setAllowedOrigins("*");
        registry.addHandler(chatWebSocketHandler(), "/ws/chat").setAllowedOrigins("*");
    }
}