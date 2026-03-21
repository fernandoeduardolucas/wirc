package com.wirc.persistence;

import com.wirc.model.ChatMessage;
import com.wirc.persistence.entity.ChatMessageEntity;
import com.wirc.persistence.entity.ChatRoomEntity;
import com.wirc.persistence.entity.RoomSessionStateEntity;
import com.wirc.persistence.repository.ChatMessageRepository;
import com.wirc.persistence.repository.ChatRoomRepository;
import com.wirc.persistence.repository.RoomSessionStateRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DatabaseChatStateStore {
    private final ChatRoomRepository chatRoomRepository;
    private final RoomSessionStateRepository roomSessionStateRepository;
    private final ChatMessageRepository chatMessageRepository;

    public DatabaseChatStateStore(
            ChatRoomRepository chatRoomRepository,
            RoomSessionStateRepository roomSessionStateRepository,
            ChatMessageRepository chatMessageRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.roomSessionStateRepository = roomSessionStateRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Transactional
    public void save(List<RoomSessionSnapshot> snapshots) {
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
