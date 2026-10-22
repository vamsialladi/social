package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class PaginationMeta {

    private String nextCursor;

    public PaginationMeta() {
        this.nextCursor = null;
    }

    public PaginationMeta(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }
}
