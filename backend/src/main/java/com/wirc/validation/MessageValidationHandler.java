package com.wirc.validation;

import com.wirc.model.ChatCommand;

// Chain of Responsibility pattern: each validator handles one rule and delegates to the next validator.
public abstract class MessageValidationHandler {
    private MessageValidationHandler next;

    public MessageValidationHandler linkWith(MessageValidationHandler nextHandler) {
        this.next = nextHandler;
        return nextHandler;
    }

    public void validate(ChatCommand command) {
        check(command);
        if (next != null) {
            next.validate(command);
        }
    }

    protected abstract void check(ChatCommand command);
}
