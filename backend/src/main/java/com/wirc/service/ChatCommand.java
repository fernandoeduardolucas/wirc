package com.wirc.service;

public record ChatCommand(String roomId, String user, String message, boolean focusedRoom) {
}
