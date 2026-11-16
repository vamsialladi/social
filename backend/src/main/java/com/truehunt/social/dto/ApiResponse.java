package com.truehunt.social.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success = true;
    private String message;
    private Map<String, String> errors;
    private T data;
    private Map<String, Object> meta;
    private ApiError error;

    public ApiResponse() {}

    public ApiResponse(T data) {
        this.data = data;
        this.success = true;
    }

    public ApiResponse(T data, Map<String, Object> meta) {
        this.data = data;
        this.meta = meta;
        this.success = true;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data);
    }

    public static <T> ApiResponse<T> success(T data, Map<String, Object> meta) {
        return new ApiResponse<>(data, meta);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setSuccess(false);
        resp.setMessage(message);
        resp.setError(new ApiError(code, message));
        return resp;
    }

    public static <T> ApiResponse<T> error(String code, String message, Map<String, String> fields) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.setSuccess(false);
        resp.setMessage(message);
        if (fields != null && !fields.isEmpty()) {
            resp.setErrors(fields);
        }
        resp.setError(new ApiError(code, message, fields));
        return resp;
    }

    public boolean isSuccess() {
        return success && error == null;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, String> getErrors() {
        return errors;
    }

    public void setErrors(Map<String, String> errors) {
        this.errors = errors;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public Map<String, Object> getMeta() {
        return meta;
    }

    public void setMeta(Map<String, Object> meta) {
        this.meta = meta;
    }

    public ApiError getError() {
        return error;
    }

    public void setError(ApiError error) {
        this.error = error;
        if (error != null) {
            this.success = false;
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ApiError {
        private String code;
        private String message;
        private Map<String, String> fields;

        public ApiError() {}

        public ApiError(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public ApiError(String code, String message, Map<String, String> fields) {
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
