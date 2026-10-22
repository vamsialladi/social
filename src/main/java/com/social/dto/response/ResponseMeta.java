package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.ALWAYS)
public class ResponseMeta {
    private String nextCursor;

    public ResponseMeta() {
    }

    public ResponseMeta(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }
}
