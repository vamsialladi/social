package com.social.dto.response;

import java.util.List;

public class CursorPageResponse<T> {

    private List<T> data;
    private String nextCursor;
    private boolean hasMore;
    private int limit;

    public CursorPageResponse() {
    }

    public CursorPageResponse(List<T> data, String nextCursor, boolean hasMore, int limit) {
        this.data = data;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.limit = limit;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private List<T> data;
        private String nextCursor;
        private boolean hasMore;
        private int limit;

        public Builder<T> data(List<T> data) {
            this.data = data;
            return this;
        }

        public Builder<T> nextCursor(String nextCursor) {
            this.nextCursor = nextCursor;
            return this;
        }

        public Builder<T> hasMore(boolean hasMore) {
            this.hasMore = hasMore;
            return this;
        }

        public Builder<T> limit(int limit) {
            this.limit = limit;
            return this;
        }

        public CursorPageResponse<T> build() {
            return new CursorPageResponse<>(data, nextCursor, hasMore, limit);
        }
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
