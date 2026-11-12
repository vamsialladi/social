package com.truehunt.social.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ConflictException extends ApiException {
    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public ConflictException(String code, String message, Map<String, String> fields) {
        super(HttpStatus.CONFLICT, code, message, fields);
    }
}

