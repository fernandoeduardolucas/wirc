package com.wirc.validation;

import com.wirc.model.ChatCommand;
import com.wirc.service.RoomSession;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class ParticipantValidationHandler extends MessageValidationHandler {
    private final Map<String, RoomSession> rooms;

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
