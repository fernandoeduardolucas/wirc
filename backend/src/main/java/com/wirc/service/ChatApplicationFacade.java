package com.wirc.service;

import com.wirc.bootstrap.DatabaseChatRoomLoader;
import com.wirc.model.AppUser;
import com.wirc.model.ChatMessage;
import com.wirc.model.ChatNotification;
import com.wirc.model.ChatRoom;
import com.wirc.model.RoomStats;
import com.wirc.model.UserMessageCount;
import com.wirc.persistence.DatabaseChatStateStore;
import com.wirc.persistence.repository.AppUserRepository;
import com.wirc.validation.MessageLengthValidationHandler;
import com.wirc.validation.MessageValidationHandler;
import com.wirc.validation.ParticipantValidationHandler;
import com.wirc.validation.RequiredFieldValidationHandler;
import com.wirc.websocket.WebSocketNotificationGateway;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SequencedMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ChatApplicationFacade {
    private final Map<String, RoomSession> rooms = new ConcurrentHashMap<>();
    private final WebSocketNotificationGateway notificationGateway;
    private final MessageValidationHandler validationChain;
    private final DatabaseChatStateStore chatStateStore;
    private final AppUserRepository appUserRepository;

    public ChatApplicationFacade(
            ChatRoomFactory roomFactory,
            DatabaseChatRoomLoader databaseChatRoomLoader,
            WebSocketNotificationGateway notificationGateway,
            DatabaseChatStateStore chatStateStore,
            AppUserRepository appUserRepository) {
        this.notificationGateway = notificationGateway;
        this.chatStateStore = chatStateStore;
        this.appUserRepository = appUserRepository;
        loadRooms(roomFactory, databaseChatRoomLoader);
        this.validationChain = buildValidationChain();
    }

    private void loadRooms(ChatRoomFactory roomFactory, DatabaseChatRoomLoader databaseChatRoomLoader) {
        databaseChatRoomLoader.loadRooms()
                .forEach(snapshot -> rooms.put(snapshot.id(), roomFactory.createFromSnapshot(snapshot)));
    }

    private MessageValidationHandler buildValidationChain() {
        MessageValidationHandler required = new RequiredFieldValidationHandler();
        required
                .linkWith(new ParticipantValidationHandler(rooms))
                .linkWith(new MessageLengthValidationHandler());
        return required;
    }

    public List<AppUser> users() {
        return appUserRepository.findAllByOrderByDisplayNameAsc().stream()
                .map(user -> new AppUser(user.getUsername(), user.getDisplayName()))
                .toList();
    }

    public List<ChatRoom> rooms() {
        return rooms.values().stream()
                .sorted(Comparator.comparing(RoomSession::name))
                .map(this::toChatRoom)
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
        room.state().onMessageSent(room, command.focusedRoom());
        persistState();

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
        room.state().onRoomFocused(room);
        persistState();
        return toChatRoom(room);
    }

    public RoomStats roomStats(String roomId) {
        RoomSession room = requireRoom(roomId);
        SequencedMap<String, Long> perUser = room.messages().stream()
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

    private ChatRoom toChatRoom(RoomSession room) {
        return new ChatRoom(
                room.id(),
                room.name(),
                new ArrayList<>(room.participants()),
                room.state().name(),
                Math.toIntExact(room.unreadMessages()));
    }

    private void persistState() {
        chatStateStore.save(rooms.values().stream()
                .sorted(Comparator.comparing(RoomSession::name))
                .map(RoomSession::snapshot)
                .toList());
    }

    private RoomSession requireRoom(String roomId) {
        RoomSession room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Sala não encontrada: " + roomId);
        }
        return room;
    }
}
