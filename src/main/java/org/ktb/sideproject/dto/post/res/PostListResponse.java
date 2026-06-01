package org.ktb.sideproject.dto.post.res;

import org.ktb.sideproject.dto.post.PageInfo;
import org.ktb.sideproject.dto.post.PostListInfo;

import java.util.List;
// 게시글 목록 조회 DTO
public record PostListResponse(
        List<PostListInfo> postListInfos,
        PageInfo pageInfo
) {
}
