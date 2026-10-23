package com.truehunt.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private MetaResponse meta;

    public ApiResponse() {}

    public ApiResponse(T data) {
        this.data = data;
        this.meta = null;
    }

    public ApiResponse(T data, MetaResponse meta) {
        this.data = data;
        this.meta = meta;
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data);
    }

    public static <T> ApiResponse<T> of(T data, String nextCursor) {
        return new ApiResponse<>(data, nextCursor != null ? new MetaResponse(nextCursor) : null);
    }

    public static <T> ApiResponse<T> of(T data, MetaResponse meta) {
        return new ApiResponse<>(data, meta);
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public MetaResponse getMeta() {
        return meta;
    }

    public void setMeta(MetaResponse meta) {
        this.meta = meta;
    }
}
