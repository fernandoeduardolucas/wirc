package com.wirc.model;

import java.util.List;

public record ChatRoom(
        String id,
        String name,
        List<String> participants,
        String state,
        int unreadMessages,
        boolean canManageMembers
) {
    public ChatRoom {
        participants = List.copyOf(participants);
    }
}
