package com.social.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CursorPageResponse<T> {

    private List<T> data = Collections.emptyList();
    private List<T> items = Collections.emptyList();
    private String nextCursor;
    private boolean hasMore;
    private int limit;
    private long totalCount;

    public CursorPageResponse() {
    }

    public CursorPageResponse(List<T> data, String nextCursor, boolean hasMore, int limit) {
        this.data = data != null ? data : Collections.emptyList();
        this.items = this.data;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.limit = limit;
        this.totalCount = this.data.size();
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static class Builder<T> {
        private List<T> data = Collections.emptyList();
        private List<T> items = Collections.emptyList();
        private String nextCursor;
        private boolean hasMore;
        private int limit;
        private long totalCount;

        public Builder<T> data(List<T> data) {
            this.data = data;
            this.items = data;
            return this;
        }

        public Builder<T> items(List<T> items) {
            this.items = items;
            this.data = items;
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

        public Builder<T> totalCount(long totalCount) {
            this.totalCount = totalCount;
            return this;
        }

        public CursorPageResponse<T> build() {
            CursorPageResponse<T> response = new CursorPageResponse<>();
            List<T> list = this.data != null && !this.data.isEmpty() ? this.data : this.items;
            response.setData(list);
            response.setItems(list);
            response.setNextCursor(this.nextCursor);
            response.setHasMore(this.hasMore);
            response.setLimit(this.limit);
            response.setTotalCount(this.totalCount != 0 ? this.totalCount : (list != null ? list.size() : 0));
            return response;
        }
    }

    public static <T> CursorPageResponse<T> empty() {
        CursorPageResponse<T> response = new CursorPageResponse<>();
        response.setData(Collections.emptyList());
        response.setItems(Collections.emptyList());
        response.setNextCursor(null);
        response.setHasMore(false);
        response.setTotalCount(0);
        return response;
    }

    public List<T> getData() {
        return data != null && !data.isEmpty() ? data : items;
    }

    public void setData(List<T> data) {
        this.data = data;
        if (this.items == null || this.items.isEmpty()) {
            this.items = data;
        }
    }

    public List<T> getItems() {
        return items != null && !items.isEmpty() ? items : data;
    }

    public void setItems(List<T> items) {
        this.items = items;
        if (this.data == null || this.data.isEmpty()) {
            this.data = items;
        }
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

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }
}
