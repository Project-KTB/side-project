package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.post.PageInfo;
import org.ktb.sideproject.dto.comment.req.CommentCreateRequest;
import org.ktb.sideproject.dto.comment.req.CommentUpdateRequest;
import org.ktb.sideproject.dto.comment.res.CommentListResponse;
import org.ktb.sideproject.dto.comment.res.CommentResponse;
import org.ktb.sideproject.entity.Comment;
import org.ktb.sideproject.entity.ImageStatus;
import org.ktb.sideproject.entity.Post;
import org.ktb.sideproject.entity.ProfileImage;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.repository.CommentRepository;
import org.ktb.sideproject.repository.PostRepository;
import org.ktb.sideproject.repository.ProfileImageRepository;
import org.ktb.sideproject.repository.UserRepository;
import org.ktb.sideproject.service.CommentService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ProfileImageRepository profileImageRepository;

    @Override
    @Transactional(readOnly = true)
    public CommentListResponse getComments(Long postId, Long cursor, int size) {
        if(!postRepository.existsById(postId)) {
            throw new CustomException(ErrorCode.POST_NOT_FOUND);
        }

        Pageable pageable = PageRequest.of(0, size + 1);
        List<Comment> comments;

        if (cursor == null) {
            comments = commentRepository.findByPostIdOrderByIdDesc(postId, pageable).getContent();
        } else {
            comments = commentRepository.findByPostIdAndIdLessThanOrderByIdDesc(postId, cursor, pageable).getContent();
        }

        boolean hasNext = comments.size() > size;

        if (hasNext) {
            comments = comments.subList(0, size);
        }

        Long nextCursor = hasNext && !comments.isEmpty()
                ? comments.getLast().getId()
                : null;

        Map<Long, String> profileImageUrlByUserId = getProfileImageUrlMap(comments);

        List<CommentResponse> commentResponses = comments.stream()
                .map(comment -> CommentResponse.from(
                        comment,
                        profileImageUrlByUserId.getOrDefault(comment.getUser().getId(), "default-profile.png")
                ))
                .toList();

        return new CommentListResponse(
                commentResponses,
                new PageInfo(hasNext, nextCursor)
        );

    }

    @Override
    @Transactional
    public CommentResponse createComment(Long userId, Long postId, CommentCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Comment comment = Comment.builder()
                .content(request.content())
                .post(post)
                .user(user)
                .build();

        Comment savedComment = commentRepository.save(comment);
        post.increaseCommentsCount(); // 동시성 문제 해결해야함

        return CommentResponse.from(savedComment, getProfileImageUrl(userId));
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentUpdateRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "댓글 작성자만 수정할 수 있습니다.");
        }

        if (!request.hasUpdateValue()) {
            throw new CustomException(ErrorCode.COMMENT_UPDATE_VALUE_REQUIRED);
        }

        comment.update(request.content());

        return CommentResponse.from(comment, getProfileImageUrl(userId));
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "댓글 작성자만 삭제할 수 있습니다.");
        }

        comment.getPost().decreaseCommentsCount(); // 동시성 문제 해결해야함
        commentRepository.delete(comment);
    }

    private String getProfileImageUrl(Long userId) {
        return profileImageRepository.findByUserIdAndStatus(userId, ImageStatus.SAVED)
                .map(ProfileImage::getImageUrl)
                .orElse("default-profile.png");
    }

    private Map<Long, String> getProfileImageUrlMap(List<Comment> comments) {
        List<Long> userIds = comments.stream()
                .map(comment -> comment.getUser().getId())
                .distinct()
                .toList();

        if (userIds.isEmpty()) {
            return Map.of();
        }

        return profileImageRepository.findByUserIdInAndStatus(userIds, ImageStatus.SAVED)
                .stream()
                .collect(Collectors.toMap(
                        profileImage -> profileImage.getUser().getId(),
                        ProfileImage::getImageUrl,
                        (first, second) -> first
                ));
    }
}
