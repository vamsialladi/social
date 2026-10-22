package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private Boolean success;
    private String message;
    private T data;
    private PaginationMeta meta;

    public ApiResponse() {
        this.success = true;
        this.meta = new PaginationMeta(null);
    }

    public ApiResponse(T data) {
        this.success = true;
        this.data = data;
        this.meta = new PaginationMeta(null);
    }

    public ApiResponse(T data, PaginationMeta meta) {
        this.success = true;
        this.data = data;
        this.meta = meta != null ? meta : new PaginationMeta(null);
    }

    public ApiResponse(Boolean success, String message, T data, PaginationMeta meta) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.meta = meta != null ? meta : new PaginationMeta(null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, new PaginationMeta(null));
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, new PaginationMeta(null));
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data, new PaginationMeta(null));
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, new PaginationMeta(null));
    }

    public static <T> ApiResponse<T> success(T data, String nextCursor) {
        return new ApiResponse<>(true, null, data, new PaginationMeta(nextCursor));
    }

    public static <T> ApiResponse<T> paginated(T data, String nextCursor) {
        return new ApiResponse<>(true, null, data, new PaginationMeta(nextCursor));
    }

    public static <T> ApiResponse<T> empty() {
        return new ApiResponse<>(true, null, null, new PaginationMeta(null));
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<?> getItems() {
        if (data instanceof List) {
            return (List<?>) data;
        }
        return null;
    }

    public PaginationMeta getMeta() {
        return meta;
    }

    public void setMeta(PaginationMeta meta) {
        this.meta = meta;
    }
}
