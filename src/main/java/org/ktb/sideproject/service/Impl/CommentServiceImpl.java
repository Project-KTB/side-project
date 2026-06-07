package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.comment.req.CommentCreateRequest;
import org.ktb.sideproject.dto.comment.req.CommentUpdateRequest;
import org.ktb.sideproject.dto.comment.res.CommentListResponse;
import org.ktb.sideproject.dto.comment.res.CommentResponse;
import org.ktb.sideproject.entity.Comment;
import org.ktb.sideproject.entity.Post;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.repository.CommentRepository;
import org.ktb.sideproject.repository.PostRepository;
import org.ktb.sideproject.repository.UserRepository;
import org.ktb.sideproject.service.CommentService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Override
    public CommentListResponse getComments(Long postId) {
        if(!postRepository.existsById(postId)) {
            throw new UsernameNotFoundException("게시글을 찾을 수 없습니다.");
        }

        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);

        List<CommentResponse> commentResponses = comments.stream()
                .map(CommentResponse::from)
                .toList();

        return new CommentListResponse(commentResponses);

    }

    @Override
    @Transactional
    public CommentResponse createComment(Long userId, Long postId, CommentCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .content(request.content())
                .post(post)
                .user(user)
                .build();

        Comment savedComment = commentRepository.save(comment);
        post.increaseCommentsCount(); // 동시성 문제 해결해야함

        return CommentResponse.from(savedComment);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentUpdateRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("댓글 작성자만 수정할 수 있습니다.");
        }

        if (!request.hasUpdateValue()) {
            throw new IllegalArgumentException("수정할 값을 입력해주세요.");
        }

        comment.update(request.content());

        return CommentResponse.from(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        if (!userRepository.existsById(userId)) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        if (!comment.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("댓글 작성자만 삭제할 수 있습니다.");
        }

        comment.getPost().decreaseCommentsCount(); // 동시성 문제 해결해야함
        commentRepository.delete(comment);
    }
}
