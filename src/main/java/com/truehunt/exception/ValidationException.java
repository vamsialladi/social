package com.truehunt.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ValidationException extends AppException {
    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    public ValidationException(String message, Map<String, String> fields) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, fields);
    }
}
