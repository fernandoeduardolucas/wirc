package com.wirc.persistence;

import com.wirc.model.ChatMessage;

import java.util.List;

public record RoomSessionSnapshot(
        String id,
        String name,
        List<String> participants,
        String state,
        long unreadMessages,
        List<ChatMessage> messages
) {
}
