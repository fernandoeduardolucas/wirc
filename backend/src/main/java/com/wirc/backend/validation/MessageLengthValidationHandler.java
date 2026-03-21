package com.wirc.backend.validation;

import com.wirc.backend.service.ChatCommand;

public class MessageLengthValidationHandler extends MessageValidationHandler {
    @Override
    protected void check(ChatCommand command) {
        if (command.message().length() > 280) {
            throw new IllegalArgumentException("A mensagem não pode ter mais de 280 caracteres.");
        }
    }
}
