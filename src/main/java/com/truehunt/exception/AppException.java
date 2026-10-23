package com.truehunt.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class AppException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Map<String, String> fields;

    public AppException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = null;
    }

    public AppException(HttpStatus status, String code, String message, Map<String, String> fields) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = fields;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Map<String, String> getFields() {
        return fields;
    }
}
