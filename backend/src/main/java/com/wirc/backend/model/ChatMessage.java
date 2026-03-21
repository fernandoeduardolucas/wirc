package com.wirc.backend.model;

import java.time.Instant;

public record ChatMessage(
        String id,
        String roomId,
        String user,
        String message,
        Instant sentAt,
        boolean highlighted
) {
}
