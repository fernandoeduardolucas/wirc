package com.wirc.validation;

import com.wirc.exception.ChatValidationException;
import com.wirc.model.ChatCommand;
import com.wirc.service.RoomSession;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
public class ParticipantValidationHandler extends MessageValidationHandler {
    private final Map<String, RoomSession> rooms;

    @Override
    protected void check(ChatCommand command) {
        RoomSession room = rooms.get(command.roomId());
        if (room == null) {
            throw new ChatValidationException(
                    "ROOM_NOT_FOUND",
                    "Sala não encontrada.",
                    Map.of("roomId", command.roomId()));
        }
        if (!room.participants().contains(command.user())) {
            throw new ChatValidationException(
                    "USER_NOT_IN_ROOM",
                    "O utilizador selecionado não pertence a esta sala.",
                    Map.of(
                            "roomId", room.id(),
                            "roomName", room.name(),
                            "user", command.user(),
                            "participants", List.copyOf(room.participants())));
        }
    }
}
