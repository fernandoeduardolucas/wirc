package com.wirc.model;

import java.util.List;

public record RoomSessionSnapshot(
        String id,
        String name,
        List<String> participants,
        String state,
        long unreadMessages,
        List<ChatMessage> messages
) {
    public RoomSessionSnapshot {
        participants = List.copyOf(participants);
        messages = List.copyOf(messages);
    }
}
