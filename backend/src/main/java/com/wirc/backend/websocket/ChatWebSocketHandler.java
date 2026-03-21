package com.wirc.backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketNotificationGateway gateway;

    public ChatWebSocketHandler(WebSocketNotificationGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        gateway.addSession(session);
        session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\",\"preview\":\"Ligação websocket estabelecida.\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Mantemos o canal simples: o cliente não precisa enviar dados para receber notificações.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        gateway.removeSession(session);
    }
}
