package com.wirc.validation;

import com.wirc.exception.ChatValidationException;
import com.wirc.model.ChatCommand;
import com.wirc.common.RoomSession;
import com.wirc.state.FocusedRoomState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticipantValidationHandlerTest {

    @Test
    void acceptsCanonicalUsernameStoredInRoomParticipants() {
        ParticipantValidationHandler handler = new ParticipantValidationHandler(Map.of(
                "room-ana-bruno",
                new RoomSession("room-ana-bruno", "Ana & Bruno", new FocusedRoomState(), List.of("ana", "bruno"))
        ));

        handler.validate(new ChatCommand("room-ana-bruno", "ana", "ana", "Olá", true));
    }

    @Test
    void returnsDetailedErrorWhenUserDoesNotBelongToRoom() {
        ParticipantValidationHandler handler = new ParticipantValidationHandler(Map.of(
                "room-ana-bruno",
                new RoomSession("room-ana-bruno", "Ana & Bruno", new FocusedRoomState(), List.of("ana", "bruno"))
        ));

        assertThatThrownBy(() -> handler.validate(new ChatCommand("room-ana-bruno", "eva", "eva", "Olá", true)))
                .isInstanceOf(ChatValidationException.class)
                .hasMessage("O utilizador selecionado não pertence a esta sala.")
                .satisfies(error -> {
                    ChatValidationException validationException = (ChatValidationException) error;
                    assertThat(validationException.code()).isEqualTo("USER_NOT_IN_ROOM");
                    assertThat(validationException.details())
                            .containsEntry("roomId", "room-ana-bruno")
                            .containsEntry("roomName", "Ana & Bruno")
                            .containsEntry("user", "eva");
                });
    }
}
