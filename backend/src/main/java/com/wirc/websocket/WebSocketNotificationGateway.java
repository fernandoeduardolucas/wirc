package com.wirc.websocket;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wirc.model.ChatNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;


import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
// Gateway pattern: isolates websocket session management and outbound notification delivery.
public class WebSocketNotificationGateway {
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public void addSession(WebSocketSession session) {
        sessions.add(session);
        log.info("Sessão WebSocket registada: sessionId={}, totalSessoes={}", session.getId(), sessions.size());
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
        log.info("Sessão WebSocket removida: sessionId={}, totalSessoes={}", session.getId(), sessions.size());
    }

    public void broadcast(ChatNotification notification) {
        String payload = serialize(notification);
        log.info("Broadcast WebSocket de mensagem: type={}, roomId={}, roomName={}, user={}, messageId={}, highlighted={}, sessoesAbertas={}",
                notification.type(), notification.roomId(), notification.roomName(), notification.user(),
                notification.messageId(), notification.highlighted(), sessions.size());

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                    log.debug("Notificação WebSocket entregue: sessionId={}, messageId={}", session.getId(), notification.messageId());
                } catch (IOException e) {
                    log.error("Falha ao enviar notificação websocket: sessionId={}, messageId={}", session.getId(), notification.messageId(), e);
                    throw new IllegalStateException("Falha ao enviar notificação websocket.", e);
                }
            } else {
                log.debug("Sessão WebSocket ignorada por estar fechada: sessionId={}", session.getId());
            }
        }
    }

    private String serialize(ChatNotification notification) {
        try {
            return objectMapper.writeValueAsString(notification);
        } catch (JsonProcessingException e) {
            log.error("Falha ao serializar notificação websocket: messageId={}, roomId={}", notification.messageId(), notification.roomId(), e);
            throw new IllegalStateException("Falha ao serializar notificação websocket.", e);
        }
    }
}
