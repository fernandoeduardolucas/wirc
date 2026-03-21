package com.wirc.websocket;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.wirc.model.ChatNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class WebSocketNotificationGateway {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public void addSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    public void broadcast(ChatNotification notification) {
        try {
            String payload = objectMapper.writeValueAsString(notification);
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(payload));
                }
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar notificação websocket.", e);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao enviar notificação websocket.", e);
        }
    }
}
