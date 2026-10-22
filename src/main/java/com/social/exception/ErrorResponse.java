package com.social.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private ApiError error;

    public ErrorResponse() {
    }

    public ErrorResponse(ApiError error) {
        this.error = error;
    }

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ApiError(code, message));
    }

    public static ErrorResponse of(String code, String message, java.util.Map<String, String> fields) {
        return new ErrorResponse(new ApiError(code, message, fields));
    }

    public ApiError getError() {
        return error;
    }

    public void setError(ApiError error) {
        this.error = error;
    }

    // Helper delegators for backward compatibility in existing code/tests
    public String getCode() {
        return error != null ? error.getCode() : null;
    }

    public String getMessage() {
        return error != null ? error.getMessage() : null;
    }
}
