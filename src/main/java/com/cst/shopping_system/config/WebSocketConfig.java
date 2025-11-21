package com.cst.shopping_system.config;

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
        // 注册一个名为 /ws 的 WebSocket 端点。
        // SockJS 用于在浏览器不支持 WebSocket 时提供备选连接。
        registry.addEndpoint("/ws").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 设置消息代理，所有发往 /app 前缀的消息都会被路由到 @MessageMapping 注解的方法。
        registry.setApplicationDestinationPrefixes("/app");
        // 启用一个简单的内存消息代理，用于将消息广播到订阅了特定主题的客户端。
        // /user 前缀用于点对点消息。
        registry.enableSimpleBroker("/topic", "/user");
        registry.setUserDestinationPrefix("/user");
    }
}