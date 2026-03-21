package com.wirc.backend.service;

public record ChatCommand(String roomId, String user, String message, boolean focusedRoom) {
}
