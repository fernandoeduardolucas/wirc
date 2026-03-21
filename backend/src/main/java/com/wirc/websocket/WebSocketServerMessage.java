package com.wirc.websocket;

import java.time.Instant;

public record WebSocketServerMessage(
        String roomId,
        String roomName,
        String preview,
        String user,
        String type,
        String messageId,
        Instant sentAt,
        boolean highlighted
) {
}
