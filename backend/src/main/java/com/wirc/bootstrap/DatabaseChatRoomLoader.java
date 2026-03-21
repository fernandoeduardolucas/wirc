package com.wirc.bootstrap;

import com.wirc.model.ChatMessage;
import com.wirc.persistence.RoomSessionSnapshot;
import com.wirc.persistence.SchemaInspector;
import com.wirc.persistence.entity.ChatRoomEntity;
import com.wirc.persistence.entity.RoomSessionStateEntity;
import com.wirc.persistence.repository.ChatMessageRepository;
import com.wirc.persistence.repository.ChatRoomRepository;
import com.wirc.persistence.repository.RoomSessionStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DatabaseChatRoomLoader {
    private static final Logger log = LoggerFactory.getLogger(DatabaseChatRoomLoader.class);
    private static final String ROOM_SESSION_STATE_TABLE = "room_session_state";
    private static final String CHAT_MESSAGE_TABLE = "chat_message";

    private final ChatRoomRepository chatRoomRepository;
    private final RoomSessionStateRepository roomSessionStateRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SchemaInspector schemaInspector;

    public DatabaseChatRoomLoader(
            ChatRoomRepository chatRoomRepository,
            RoomSessionStateRepository roomSessionStateRepository,
            ChatMessageRepository chatMessageRepository,
            SchemaInspector schemaInspector) {
        this.chatRoomRepository = chatRoomRepository;
        this.roomSessionStateRepository = roomSessionStateRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.schemaInspector = schemaInspector;
    }

    public List<RoomSessionSnapshot> loadRooms() {
        Map<String, RoomSessionStateEntity> sessionStates = loadSessionStates();
        Map<String, List<ChatMessage>> messagesByRoom = loadMessagesByRoom();

        return chatRoomRepository.findAllByOrderByNameAsc().stream()
                .map(room -> toSnapshot(room, sessionStates.get(room.getId()), messagesByRoom.getOrDefault(room.getId(), List.of())))
                .toList();
    }

    private Map<String, RoomSessionStateEntity> loadSessionStates() {
        if (!schemaInspector.tableExists(ROOM_SESSION_STATE_TABLE)) {
            log.warn("Table '{}' was not found. Starting with default in-memory room session state until migrations are applied.", ROOM_SESSION_STATE_TABLE);
            return Map.of();
        }

        return roomSessionStateRepository.findAll().stream()
                .collect(Collectors.toMap(RoomSessionStateEntity::getRoomId, Function.identity()));
    }

    private Map<String, List<ChatMessage>> loadMessagesByRoom() {
        if (!schemaInspector.tableExists(CHAT_MESSAGE_TABLE)) {
            log.warn("Table '{}' was not found. Starting with empty room message history until migrations are applied.", CHAT_MESSAGE_TABLE);
            return Map.of();
        }

        return chatMessageRepository.findAll().stream()
                .map(message -> new ChatMessage(
                        message.getId(),
                        message.getRoom().getId(),
                        message.getUsername(),
                        message.getMessage(),
                        message.getSentAt(),
                        message.isHighlighted()))
                .collect(Collectors.groupingBy(ChatMessage::roomId));
    }

    private RoomSessionSnapshot toSnapshot(
            ChatRoomEntity room,
            RoomSessionStateEntity sessionState,
            List<ChatMessage> messages) {
        List<String> participants = room.getMembers().stream()
                .map(member -> member.getUser().getDisplayName())
                .sorted(Comparator.naturalOrder())
                .toList();

        if (sessionState == null) {
            return new RoomSessionSnapshot(room.getId(), room.getName(), participants, "FOCUSED", 0, messages);
        }

        return new RoomSessionSnapshot(
                room.getId(),
                room.getName(),
                participants,
                sessionState.getState(),
                sessionState.getUnreadMessages(),
                messages);
    }
}
