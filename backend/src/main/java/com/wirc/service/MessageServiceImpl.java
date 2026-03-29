package com.wirc.service;

import com.wirc.common.DatabaseChatStateStore;
import com.wirc.common.RoomSession;
import com.wirc.gateway.WebSocketNotificationGateway;
import com.wirc.model.ChatCommand;
import com.wirc.model.ChatMessage;
import com.wirc.model.ChatNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    private static final List<String> HIGHLIGHT_KEYWORDS = List.of("graphql", "websocket");

    private final ChatStateRegistry chatStateRegistry;
    private final WebSocketNotificationGateway notificationGateway;
    private final DatabaseChatStateStore chatStateStore;
    private final UserService userService;
    private final RoomService roomService;


    @Override
    public List<ChatMessage> messagesByRoom(String roomId, String activeUser) {
        RoomSession room = roomService.requireAccessibleRoom(roomId, activeUser);
        return new ArrayList<>(room.messages());
    }

    @Override
    public List<ChatMessage> searchMessages(String term, String activeUser) {
        String canonicalUser = userService.requireCanonicalUser(activeUser);
        String normalized = term.toLowerCase(Locale.ROOT);
        return chatStateRegistry.rooms().values().stream()
                .filter(room -> room.participants().contains(canonicalUser))
                .flatMap(room -> room.messages().stream())
                .filter(message -> message.message().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }

    @Override
    public ChatMessage sendMessage(ChatCommand command) {
        ChatCommand validatedCommand = validateCommand(command);
        RoomSession room = chatStateRegistry.requireRoom(validatedCommand.roomId());
        log.info("Processando mensagem entre utilizadores: roomId={}, roomName={}, user={}, focusedRoom={}, messageLength={}",
                room.id(), room.name(), validatedCommand.user(), validatedCommand.focusedRoom(), validatedCommand.message().length());
        ChatMessage chatMessage = createChatMessage(room, validatedCommand.user(), validatedCommand.message());

        room.messages().add(chatMessage);
        room.state().onMessageSent(room, validatedCommand.focusedRoom());
        persistState();
        broadcastNewMessage(room, chatMessage);
        log.info("Mensagem persistida e difundida: roomId={}, messageId={}, user={}, highlighted={}",
                room.id(), chatMessage.id(), chatMessage.user(), chatMessage.highlighted());

        return chatMessage;
    }

    private ChatCommand validateCommand(ChatCommand command) {
        String canonicalUsername = userService.resolveCanonicalUsername(command.user())
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado: " + command.user()));
        String canonicalActiveUser = userService.requireCanonicalUser(command.activeUser());
        if (!canonicalActiveUser.equals(canonicalUsername)) {
            throw new IllegalArgumentException("Só o utilizador autenticado pode enviar mensagens em seu nome.");
        }
        ChatCommand validatedCommand = new ChatCommand(
                command.roomId(),
                canonicalActiveUser,
                canonicalUsername,
                command.message(),
                command.focusedRoom());
        chatStateRegistry.validationChain().validate(validatedCommand);
        return validatedCommand;
    }

    private ChatMessage createChatMessage(RoomSession room, String canonicalUsername, String message) {
        return new ChatMessage(
                UUID.randomUUID().toString(),
                room.id(),
                canonicalUsername,
                message,
                Instant.now(),
                isHighlighted(message));
    }

    private boolean isHighlighted(String message) {
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        return HIGHLIGHT_KEYWORDS.stream().anyMatch(normalizedMessage::contains);
    }

    private void broadcastNewMessage(RoomSession room, ChatMessage chatMessage) {
        notificationGateway.broadcast(new ChatNotification(
                room.id(),
                room.name(),
                chatMessage.message(),
                chatMessage.user(),
                "NEW_MESSAGE",
                chatMessage.id(),
                chatMessage.sentAt(),
                chatMessage.highlighted()
        ));
    }

    private void persistState() {
        chatStateStore.save(chatStateRegistry.sortedRooms().stream()
                .map(RoomSession::snapshot)
                .toList());
    }
}
