package com.wirc.validation;

import com.wirc.model.ChatCommand;

public class RequiredFieldValidationHandler extends MessageValidationHandler {
    @Override
    protected void check(ChatCommand command) {
        if (command.roomId().isBlank() || command.user().isBlank() || command.message().isBlank()) {
            throw new IllegalArgumentException("Sala, utilizador e mensagem são obrigatórios.");
        }
    }
}
