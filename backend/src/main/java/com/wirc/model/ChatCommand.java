package com.wirc.model;

public record ChatCommand(
        String roomId,
        String user,
        String message,
        boolean focusedRoom
) {
}
