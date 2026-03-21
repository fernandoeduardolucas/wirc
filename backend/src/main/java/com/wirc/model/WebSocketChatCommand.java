package com.wirc.model;

public record WebSocketChatCommand(
        String type,
        String roomId,
        String user,
        String message,
        boolean focusedRoom
) {
}
