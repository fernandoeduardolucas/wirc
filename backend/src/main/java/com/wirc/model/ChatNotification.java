package com.wirc.model;

public record ChatNotification(
        String roomId,
        String roomName,
        String preview,
        String user,
        String type
) {
}
