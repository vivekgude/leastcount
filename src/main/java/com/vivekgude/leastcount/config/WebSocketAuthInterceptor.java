package com.vivekgude.leastcount.config;

import com.vivekgude.leastcount.security.CustomUserDetails;
import com.vivekgude.leastcount.security.JWTUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

import static com.vivekgude.leastcount.constants.AuthConstants.*;
import static com.vivekgude.leastcount.constants.Constants.*;

@Component
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JWTUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Autowired
    public WebSocketAuthInterceptor(JWTUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                 WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
            
            // Get token from Authorization header
            String authHeader = servletRequest.getServletRequest().getHeader(AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith(BEARER)) {
                log.error("WebSocket connection attempt without valid Authorization header");
                return false;
            }

            String token = authHeader.substring(BEARER.length());

            try {
                // Extract username from token
                String username = jwtUtils.extractUsername(token);
                if (username != null) {
                    // Load user details and validate token
                    CustomUserDetails userDetails = (CustomUserDetails) userDetailsService.loadUserByUsername(username);
                    if (jwtUtils.validateToken(token, userDetails)) {
                        attributes.put(USERNAME, username);
                        attributes.put(USERID, userDetails.getUserId());
                        return true;
                    } else {
                        log.error("Invalid token for user: {}", username);
                        return false;
                    }
                }
            } catch (Exception e) {
                log.error("Error validating WebSocket token: {}", e.getMessage());
            }
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                             WebSocketHandler wsHandler, Exception exception) {
        // No cleanup needed
    }
} 