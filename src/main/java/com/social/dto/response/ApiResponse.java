package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class ApiResponse<T> {

    private T data;
    private ResponseMeta meta;

    public ApiResponse() {
    }

    public ApiResponse(T data, ResponseMeta meta) {
        this.data = data;
        this.meta = meta != null ? meta : new ResponseMeta(null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(data, new ResponseMeta(null));
    }

    public static <T> ApiResponse<T> success(T data, String nextCursor) {
        return new ApiResponse<>(data, new ResponseMeta(nextCursor));
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public ResponseMeta getMeta() {
        return meta;
    }

    public void setMeta(ResponseMeta meta) {
        this.meta = meta;
    }
}
