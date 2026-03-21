package com.wirc.backend.service;

import com.wirc.backend.model.ChatMessage;
import com.wirc.backend.model.ChatNotification;
import com.wirc.backend.model.ChatRoom;
import com.wirc.backend.model.RoomStats;
import com.wirc.backend.model.UserMessageCount;
import com.wirc.backend.validation.MessageLengthValidationHandler;
import com.wirc.backend.validation.MessageValidationHandler;
import com.wirc.backend.validation.ParticipantValidationHandler;
import com.wirc.backend.validation.RequiredFieldValidationHandler;
import com.wirc.backend.websocket.WebSocketNotificationGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatApplicationFacade {
    private final Map<String, RoomSession> rooms = new ConcurrentHashMap<>();
    private final WebSocketNotificationGateway notificationGateway;
    private final MessageValidationHandler validationChain;

    public ChatApplicationFacade(ChatRoomFactory roomFactory, WebSocketNotificationGateway notificationGateway) {
        this.notificationGateway = notificationGateway;
        seedRooms(roomFactory);
        this.validationChain = buildValidationChain();
    }

    private void seedRooms(ChatRoomFactory roomFactory) {
        rooms.put("room-ana-bruno", roomFactory.create("room-ana-bruno", "Ana & Bruno", List.of("Ana", "Bruno")));
        rooms.put("room-equipa", roomFactory.create("room-equipa", "Equipa Projeto", List.of("Ana", "Bruno", "Carla", "Diogo")));
        rooms.put("room-estudo", roomFactory.create("room-estudo", "Grupo de Estudo", List.of("Carla", "Diogo", "Eva")));
    }

    private MessageValidationHandler buildValidationChain() {
        // Design Pattern: Chain of Responsibility -> cada handler valida uma regra antes do envio.
        MessageValidationHandler required = new RequiredFieldValidationHandler();
        required
                .linkWith(new ParticipantValidationHandler(rooms))
                .linkWith(new MessageLengthValidationHandler());
        return required;
    }

    // Design Pattern: Facade -> expõe uma API única para GraphQL/controllers consumirem sem conhecer a lógica interna.
    public List<ChatRoom> rooms() {
        return rooms.values().stream()
                .sorted(Comparator.comparing(RoomSession::name))
                .map(room -> new ChatRoom(
                        room.id(),
                        room.name(),
                        new ArrayList<>(room.participants()),
                        room.state().name(),
                        Math.toIntExact(room.unreadMessages())))
                .toList();
    }

    public List<ChatMessage> messagesByRoom(String roomId) {
        return new ArrayList<>(requireRoom(roomId).messages());
    }

    public List<ChatMessage> searchMessages(String term) {
        String normalized = term.toLowerCase(Locale.ROOT);
        return rooms.values().stream()
                .flatMap(room -> room.messages().stream())
                .filter(message -> message.message().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }

    public ChatMessage sendMessage(ChatCommand command) {
        validationChain.validate(command);
        RoomSession room = requireRoom(command.roomId());
        boolean highlighted = command.message().toLowerCase(Locale.ROOT).contains("graphql")
                || command.message().toLowerCase(Locale.ROOT).contains("websocket");

        ChatMessage chatMessage = new ChatMessage(
                UUID.randomUUID().toString(),
                room.id(),
                command.user(),
                command.message(),
                Instant.now(),
                highlighted);

        room.messages().add(chatMessage);
        room.state().onMessageSent(room, command.focusedRoom()); // Design Pattern: State -> o comportamento muda conforme o estado atual da sala.

        if (!command.focusedRoom()) {
            notificationGateway.broadcast(new ChatNotification(
                    room.id(),
                    room.name(),
                    command.message(),
                    command.user(),
                    "NEW_MESSAGE"
            ));
        }

        return chatMessage;
    }

    public ChatRoom focusRoom(String roomId) {
        RoomSession room = requireRoom(roomId);
        room.state().onRoomFocused(room); // Design Pattern: State -> focar a sala reseta notificações e pode mudar de estado.
        return new ChatRoom(room.id(), room.name(), new ArrayList<>(room.participants()), room.state().name(), Math.toIntExact(room.unreadMessages()));
    }

    public RoomStats roomStats(String roomId) {
        RoomSession room = requireRoom(roomId);
        Map<String, Long> perUser = room.messages().stream()
                .collect(Collectors.groupingBy(ChatMessage::user, LinkedHashMap::new, Collectors.counting()));

        String busiestUser = perUser.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");

        long highlighted = room.messages().stream().filter(ChatMessage::highlighted).count();
        return new RoomStats(room.id(), room.name(), room.messages().size(), Math.toIntExact(highlighted), busiestUser);
    }

    public List<UserMessageCount> topUsers() {
        return rooms.values().stream()
                .flatMap(room -> room.messages().stream())
                .collect(Collectors.groupingBy(ChatMessage::user, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> new UserMessageCount(entry.getKey(), Math.toIntExact(entry.getValue())))
                .toList();
    }

    private RoomSession requireRoom(String roomId) {
        RoomSession room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Sala não encontrada: " + roomId);
        }
        return room;
    }
}
