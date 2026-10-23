package com.truehunt.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

public class ErrorResponse {
    private ErrorDetail error;

    public ErrorResponse() {}

    public ErrorResponse(String code, String message) {
        this.error = new ErrorDetail(code, message, null);
    }

    public ErrorResponse(String code, String message, Map<String, String> fields) {
        this.error = new ErrorDetail(code, message, fields);
    }

    public ErrorDetail getError() {
        return error;
    }

    public void setError(ErrorDetail error) {
        this.error = error;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ErrorDetail {
        private String code;
        private String message;
        private Map<String, String> fields;

        public ErrorDetail() {}

        public ErrorDetail(String code, String message, Map<String, String> fields) {
            this.code = code;
            this.message = message;
            this.fields = fields;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Map<String, String> getFields() {
            return fields;
        }

        public void setFields(Map<String, String> fields) {
            this.fields = fields;
        }
    }
}
