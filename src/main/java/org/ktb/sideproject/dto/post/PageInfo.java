package org.ktb.sideproject.dto.post;

public record PageInfo(
        Boolean hasNext,
        Long nextCursor
) {
}
