package com.vivekgude.leastcount.config;

import com.vivekgude.leastcount.handler.WebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final WebSocketHandler webSocketHandler;
    private final WebSocketAuthInterceptor authInterceptor;

    public WebSocketConfig(WebSocketHandler webSocketHandler, WebSocketAuthInterceptor authInterceptor) {
        this.webSocketHandler = webSocketHandler;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler, "/ws")
                .addInterceptors(authInterceptor)
                .setAllowedOrigins("*");
    }
} 