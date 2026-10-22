package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private T data;
    private PaginationMeta meta;

    public ApiResponse() {
        this.meta = new PaginationMeta(null);
    }

    public ApiResponse(T data) {
        this.data = data;
        this.meta = new PaginationMeta(null);
    }

    public ApiResponse(T data, PaginationMeta meta) {
        this.data = data;
        this.meta = meta != null ? meta : new PaginationMeta(null);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, new PaginationMeta(null));
    }

    public static <T> ApiResponse<T> paginated(T data, String nextCursor) {
        return new ApiResponse<>(data, new PaginationMeta(nextCursor));
    }

    public static <T> ApiResponse<T> empty() {
        return new ApiResponse<>(null, new PaginationMeta(null));
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public PaginationMeta getMeta() {
        return meta;
    }

    public void setMeta(PaginationMeta meta) {
        this.meta = meta;
    }
}
