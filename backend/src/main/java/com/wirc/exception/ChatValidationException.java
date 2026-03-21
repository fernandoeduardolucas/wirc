package com.wirc.exception;

import java.util.Map;

public class ChatValidationException extends IllegalArgumentException {
    private final String code;
    private final Map<String, Object> details;

    public ChatValidationException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = Map.copyOf(details);
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
