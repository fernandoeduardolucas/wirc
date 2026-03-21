package com.wirc.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wirc.model.ChatCommand;
import com.wirc.service.ChatApplicationFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
@RequiredArgsConstructor
// Observer pattern support: registers and unregisters websocket clients that receive chat updates.
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private final WebSocketNotificationGateway gateway;
    private final ChatApplicationFacade chatApplicationFacade;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        gateway.addSession(session);
        session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\",\"preview\":\"Ligação websocket estabelecida.\",\"highlighted\":false}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            WebSocketChatCommand command = objectMapper.readValue(message.getPayload(), WebSocketChatCommand.class);
            if (!"SEND_MESSAGE".equals(command.type())) {
                session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"preview\":\"Tipo de mensagem websocket não suportado.\",\"highlighted\":false}"));
                return;
            }

            chatApplicationFacade.sendMessage(new ChatCommand(
                    command.roomId(),
                    command.user(),
                    command.message(),
                    command.focusedRoom()
            ));
        } catch (JsonProcessingException e) {
            session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"preview\":\"Payload websocket inválido.\",\"highlighted\":false}"));
        } catch (IllegalArgumentException e) {
            sendError(session, e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        gateway.removeSession(session);
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
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
