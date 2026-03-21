package com.wirc.backend.validation;

import com.wirc.backend.service.ChatCommand;
import com.wirc.backend.service.RoomSession;

import java.util.Map;

public class ParticipantValidationHandler extends MessageValidationHandler {
    private final Map<String, RoomSession> rooms;

    public ParticipantValidationHandler(Map<String, RoomSession> rooms) {
        this.rooms = rooms;
    }

    @Override
    protected void check(ChatCommand command) {
        RoomSession room = rooms.get(command.roomId());
        if (room == null) {
            throw new IllegalArgumentException("Sala não encontrada.");
        }
        if (!room.participants().contains(command.user())) {
            throw new IllegalArgumentException("O utilizador não pertence à sala selecionada.");
        }
    }
}
