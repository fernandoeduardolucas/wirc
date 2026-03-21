package com.wirc.websocket;

public record WebSocketChatCommand(
        String type,
        String roomId,
        String user,
        String message,
        boolean focusedRoom
) {
}
