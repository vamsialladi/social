package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private ErrorDetails error;
    private Integer status;
    private String message;
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

    public static ErrorResponse of(String code, String message, int status) {
        ErrorDetails details = new ErrorDetails(code, message);
        return new ErrorResponse(details, status);
    }

    public static ErrorResponse of(String code, String message, Map<String, String> fields, int status) {
        ErrorDetails details = new ErrorDetails(code, message, fields);
        return new ErrorResponse(details, status);
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

    public Map<String, String> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(Map<String, String> validationErrors) {
        this.validationErrors = validationErrors;
    }
}
