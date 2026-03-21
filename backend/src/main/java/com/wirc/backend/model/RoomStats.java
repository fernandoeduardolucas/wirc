package com.wirc.backend.model;

public record RoomStats(
        String roomId,
        String roomName,
        int totalMessages,
        int highlightedMessages,
        String busiestUser
) {
}
