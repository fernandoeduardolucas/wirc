package com.wirc.validation;

import com.wirc.model.ChatCommand;

// Chain of Responsibility pattern: validates the message length after previous checks succeed.
public class MessageLengthValidationHandler extends MessageValidationHandler {
    @Override
    protected void check(ChatCommand command) {
        if (command.message().length() > 280) {
            throw new IllegalArgumentException("A mensagem não pode ter mais de 280 caracteres.");
        }
    }
}
