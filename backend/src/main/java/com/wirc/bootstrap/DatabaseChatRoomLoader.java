package com.wirc.bootstrap;

import com.wirc.persistence.repository.ChatRoomRepository;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DatabaseChatRoomLoader {
    private final ChatRoomRepository chatRoomRepository;

    public DatabaseChatRoomLoader(ChatRoomRepository chatRoomRepository) {
        this.chatRoomRepository = chatRoomRepository;
    }

    public List<ChatRoomSeed> loadRooms() {
        return chatRoomRepository.findAllByOrderByNameAsc().stream()
                .map(room -> new ChatRoomSeed(
                        room.getId(),
                        room.getName(),
                        room.getMembers().stream()
                                .map(member -> member.getUser().getDisplayName())
                                .sorted(Comparator.naturalOrder())
                                .toList()))
                .toList();
    }
}
