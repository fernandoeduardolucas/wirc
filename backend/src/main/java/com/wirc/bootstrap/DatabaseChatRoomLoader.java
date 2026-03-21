package com.wirc.bootstrap;

import com.wirc.model.ChatMessage;
import com.wirc.persistence.RoomSessionSnapshot;
import com.wirc.persistence.entity.ChatRoomEntity;
import com.wirc.persistence.entity.RoomSessionStateEntity;
import com.wirc.persistence.repository.ChatMessageRepository;
import com.wirc.persistence.repository.ChatRoomRepository;
import com.wirc.persistence.repository.RoomSessionStateRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DatabaseChatRoomLoader {
    private final ChatRoomRepository chatRoomRepository;
    private final RoomSessionStateRepository roomSessionStateRepository;
    private final ChatMessageRepository chatMessageRepository;

    public DatabaseChatRoomLoader(
            ChatRoomRepository chatRoomRepository,
            RoomSessionStateRepository roomSessionStateRepository,
            ChatMessageRepository chatMessageRepository) {
        this.chatRoomRepository = chatRoomRepository;
        this.roomSessionStateRepository = roomSessionStateRepository;
        this.chatMessageRepository = chatMessageRepository;
    }

    public List<RoomSessionSnapshot> loadRooms() {
        Map<String, RoomSessionStateEntity> sessionStates = roomSessionStateRepository.findAll().stream()
                .collect(Collectors.toMap(RoomSessionStateEntity::getRoomId, Function.identity()));

        return chatRoomRepository.findAllByOrderByNameAsc().stream()
                .map(room -> toSnapshot(room, sessionStates.get(room.getId())))
                .toList();
    }

    private RoomSessionSnapshot toSnapshot(ChatRoomEntity room, RoomSessionStateEntity sessionState) {
        List<String> participants = room.getMembers().stream()
                .map(member -> member.getUser().getDisplayName())
                .sorted(Comparator.naturalOrder())
                .toList();

        List<ChatMessage> messages = chatMessageRepository.findAllByRoom_IdOrderBySentAtAsc(room.getId()).stream()
                .map(message -> new ChatMessage(
                        message.getId(),
                        room.getId(),
                        message.getUsername(),
                        message.getMessage(),
                        message.getSentAt(),
                        message.isHighlighted()))
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
