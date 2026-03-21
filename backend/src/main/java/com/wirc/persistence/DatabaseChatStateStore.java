package com.wirc.persistence;

import com.wirc.model.ChatMessage;
import com.wirc.entity.ChatMessageEntity;
import com.wirc.entity.ChatRoomEntity;
import com.wirc.entity.RoomSessionStateEntity;
import com.wirc.model.RoomSessionSnapshot;
import com.wirc.repository.ChatMessageRepository;
import com.wirc.repository.ChatRoomRepository;
import com.wirc.repository.RoomSessionStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseChatStateStore {
    private static final String ROOM_SESSION_STATE_TABLE = "room_session_state";
    private static final String CHAT_MESSAGE_TABLE = "chat_message";

    private final ChatRoomRepository chatRoomRepository;
    private final RoomSessionStateRepository roomSessionStateRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final SchemaInspector schemaInspector;

    @Transactional
    public void save(List<RoomSessionSnapshot> snapshots) {
        if (!schemaInspector.tableExists(ROOM_SESSION_STATE_TABLE) || !schemaInspector.tableExists(CHAT_MESSAGE_TABLE)) {
            log.warn("Skipping chat state persistence because required chat tables are missing. Apply the latest database migrations to enable persistence.");
            return;
        }

        Map<String, ChatRoomEntity> roomsById = chatRoomRepository.findAllById(
                        snapshots.stream().map(RoomSessionSnapshot::id).toList())
                .stream()
                .collect(Collectors.toMap(ChatRoomEntity::getId, Function.identity()));

        roomSessionStateRepository.saveAll(snapshots.stream()
                .map(snapshot -> new RoomSessionStateEntity(
                        snapshot.id(),
                        snapshot.state(),
                        snapshot.unreadMessages()))
                .toList());

        snapshots.forEach(snapshot -> {
            ChatRoomEntity room = roomsById.get(snapshot.id());
            if (room == null) {
                throw new IllegalStateException("Sala não encontrada para persistência: " + snapshot.id());
            }
            chatMessageRepository.deleteAllByRoom_Id(snapshot.id());
            chatMessageRepository.saveAll(snapshot.messages().stream()
                    .map(message -> toEntity(message, room))
                    .toList());
        });
    }

    private ChatMessageEntity toEntity(ChatMessage message, ChatRoomEntity room) {
        return new ChatMessageEntity(
                message.id(),
                room,
                message.user(),
                message.message(),
                message.sentAt(),
                message.highlighted());
    }
}
