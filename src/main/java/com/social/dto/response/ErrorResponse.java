package com.social.dto.response;

import java.util.Map;

public class ErrorResponse {

    private ErrorDetails error;

    public ErrorResponse() {
    }

    public ErrorResponse(ErrorDetails error) {
        this.error = error;
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ErrorDetails(code, message));
    }

    public static ErrorResponse of(String code, String message, Map<String, String> fields) {
        return new ErrorResponse(new ErrorDetails(code, message, fields));
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
    }
}
