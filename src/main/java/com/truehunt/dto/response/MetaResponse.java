package com.truehunt.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetaResponse {
    private String nextCursor;
    private Long total;

    public MetaResponse() {}

    public MetaResponse(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public MetaResponse(String nextCursor, Long total) {
        this.nextCursor = nextCursor;
        this.total = total;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}
