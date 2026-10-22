package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private ErrorDetails error;
    private Integer status;
    private String message;
    private String path;
    private Instant timestamp = Instant.now();
    private Map<String, String> validationErrors;

    public ErrorResponse() {
    }

    public ErrorResponse(ErrorDetails error) {
        this.error = error;
        if (error != null) {
            this.message = error.getMessage();
            this.validationErrors = error.getFields();
        }
    }

    public ErrorResponse(ErrorDetails error, int status) {
        this.error = error;
        this.status = status;
        if (error != null) {
            this.message = error.getMessage();
            this.validationErrors = error.getFields();
        }
    }

    public ErrorResponse(int status, String errorStr, String message, String path, Instant timestamp, Map<String, String> validationErrors) {
        this.status = status;
        this.error = new ErrorDetails(errorStr != null ? errorStr : "ERROR", message, validationErrors);
        this.message = message;
        this.path = path;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.validationErrors = validationErrors;
    }

    public static ErrorResponse of(String code, String message) {
        int status = mapCodeToStatus(code);
        ErrorDetails details = new ErrorDetails(code, message);
        ErrorResponse resp = new ErrorResponse(details, status);
        resp.setMessage(message);
        return resp;
    }

    public static ErrorResponse of(String code, String message, Map<String, String> fields) {
        ErrorDetails details = new ErrorDetails(code, message, fields);
        ErrorResponse resp = new ErrorResponse(details, 400);
        resp.setMessage(message);
        resp.setValidationErrors(fields);
        return resp;
    }

    public static ErrorResponse of(String code, String message, int status) {
        ErrorDetails details = new ErrorDetails(code, message);
        ErrorResponse resp = new ErrorResponse(details, status);
        resp.setMessage(message);
        return resp;
    }

    public static ErrorResponse of(String code, String message, Map<String, String> fields, int status) {
        ErrorDetails details = new ErrorDetails(code, message, fields);
        ErrorResponse resp = new ErrorResponse(details, status);
        resp.setMessage(message);
        resp.setValidationErrors(fields);
        return resp;
    }

    private static int mapCodeToStatus(String code) {
        if (code == null) return 500;
        return switch (code) {
            case "NOT_FOUND" -> 404;
            case "FORBIDDEN" -> 403;
            case "NOT_AUTHENTICATED", "UNAUTHORIZED" -> 401;
            case "CONFLICT" -> 409;
            case "RATE_LIMIT_EXCEEDED" -> 429;
            case "INVALID_INPUT", "VALIDATION_ERROR" -> 400;
            default -> 500;
        };
    }

    public ErrorDetails getError() {
        return error;
    }

    public void setError(ErrorDetails error) {
        this.error = error;
        if (error != null) {
            this.message = error.getMessage();
            this.validationErrors = error.getFields();
        }
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
        if (this.error != null && this.error.getFields() == null) {
            this.error.setFields(validationErrors);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int status;
        private String error;
        private String message;
        private String path;
        private Instant timestamp = Instant.now();
        private Map<String, String> validationErrors;

        public Builder status(int status) { this.status = status; return this; }
        public Builder error(String error) { this.error = error; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder path(String path) { this.path = path; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder validationErrors(Map<String, String> validationErrors) { this.validationErrors = validationErrors; return this; }
        public ErrorResponse build() {
            return new ErrorResponse(status, error, message, path, timestamp, validationErrors);
        }
    }
}
