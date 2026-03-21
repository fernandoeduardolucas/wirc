package com.wirc.bootstrap;

import com.wirc.model.ChatMessage;
import com.wirc.model.RoomSessionSnapshot;
import com.wirc.persistence.SchemaInspector;
import com.wirc.entity.ChatRoomEntity;
import com.wirc.entity.RoomSessionStateEntity;
import com.wirc.repository.ChatMessageRepository;
import com.wirc.repository.ChatRoomRepository;
import com.wirc.repository.RoomSessionStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseChatRoomLoader {
    private static final String ROOM_SESSION_STATE_TABLE = "room_session_state";
    private static final String CHAT_MESSAGE_TABLE = "chat_message";

    private final ChatRoomRepository chatRoomRepository;
    private final RoomSessionStateRepository roomSessionStateRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SchemaInspector schemaInspector;

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
                .map(member -> member.getUser().getUsername())
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
