package com.wirc.model;

public record ChatCommand(
        String roomId,
        String activeUser,
        String user,
        String message,
        boolean focusedRoom
) {
}
