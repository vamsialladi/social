package com.social.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CursorPageResponse<T> {

    @Builder.Default
    private List<T> items = Collections.emptyList();
    private String nextCursor;
    private boolean hasMore;
    private long totalCount;

    public static <T> CursorPageResponse<T> empty() {
        return CursorPageResponse.<T>builder()
                .items(Collections.emptyList())
                .nextCursor(null)
                .hasMore(false)
                .totalCount(0)
                .build();
    }
}
