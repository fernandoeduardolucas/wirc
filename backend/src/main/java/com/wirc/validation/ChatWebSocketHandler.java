package com.wirc.validation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wirc.gateway.WebSocketNotificationGateway;
import com.wirc.model.ChatCommand;
import com.wirc.service.ChatApplication;
import com.wirc.model.WebSocketChatCommand;
import com.wirc.model.WebSocketServerMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
// Observer pattern support: registers and unregisters websocket clients that receive chat updates.
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final WebSocketNotificationGateway gateway;
    private final ChatApplication chatApplication;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket handshake concluído: sessionId={}, remoteAddress={}, uri={}",
                session.getId(), session.getRemoteAddress(), session.getUri());
        gateway.addSession(session);
        session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\",\"preview\":\"Ligação websocket estabelecida.\",\"highlighted\":false}"));
        log.info("Mensagem CONNECTED enviada após handshake: sessionId={}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("Mensagem WebSocket recebida: sessionId={}, payload={}", session.getId(), message.getPayload());
        try {
            WebSocketChatCommand command = objectMapper.readValue(message.getPayload(), WebSocketChatCommand.class);
            if (!"SEND_MESSAGE".equals(command.type())) {
                log.warn("Tipo de mensagem WebSocket não suportado: sessionId={}, type={}", session.getId(), command.type());
                session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"preview\":\"Tipo de mensagem websocket não suportado.\",\"highlighted\":false}"));
                return;
            }

            log.info("Encaminhando mensagem entre utilizadores: sessionId={}, roomId={}, user={}, focusedRoom={}",
                    session.getId(), command.roomId(), command.user(), command.focusedRoom());
            chatApplication.sendMessage(new ChatCommand(
                    command.roomId(),
                    command.user(),
                    command.message(),
                    command.focusedRoom()
            ));
        } catch (JsonProcessingException e) {
            log.warn("Payload WebSocket inválido: sessionId={}, erro={}", session.getId(), e.getOriginalMessage());
            session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"preview\":\"Payload websocket inválido.\",\"highlighted\":false}"));
        } catch (IllegalArgumentException e) {
            log.warn("Erro de validação ao processar mensagem WebSocket: sessionId={}, erro={}", session.getId(), e.getMessage());
            sendError(session, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        gateway.removeSession(session);
        log.info("WebSocket fechado: sessionId={}, statusCode={}, reason={}",
                session.getId(), status.getCode(), status.getReason());
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        log.info("Enviando erro via WebSocket: sessionId={}, erro={}", session.getId(), message);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new WebSocketServerMessage(
                null,
                null,
                message,
                null,
                "ERROR",
                null,
                null,
                false
        ))));
    }
}
