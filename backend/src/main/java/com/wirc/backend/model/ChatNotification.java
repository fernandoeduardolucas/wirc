package com.wirc.backend.model;

public record ChatNotification(
        String roomId,
        String roomName,
        String preview,
        String user,
        String type
) {
}
