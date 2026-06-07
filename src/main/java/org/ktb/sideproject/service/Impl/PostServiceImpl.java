package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.post.PageInfo;
import org.ktb.sideproject.dto.post.PostListInfo;
import org.ktb.sideproject.dto.post.req.PostCreateRequest;
import org.ktb.sideproject.dto.post.req.PostUpdateRequest;
import org.ktb.sideproject.dto.post.res.PostListResponse;
import org.ktb.sideproject.dto.post.res.PostDetailResponse;
import org.ktb.sideproject.dto.post.res.PostUpdateResponse;
import org.ktb.sideproject.entity.Post;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.repository.PostLikeRepository;
import org.ktb.sideproject.repository.PostRepository;
import org.ktb.sideproject.repository.UserRepository;
import org.ktb.sideproject.service.PostService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;

    // 게시글 생성
    @Override
    @Transactional
    public PostDetailResponse createPost(Long userId, PostCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        //이미지 업로드 해서 URL 받기

        Post post = Post.builder()
                .content(request.content())
                .title(request.title())
                .user(user)
                .build();

        Post currentPost = postRepository.save(post);

        return PostDetailResponse.from(currentPost, false);
    }

    // 게시글 목록 조회
    @Override
    @Transactional(readOnly = true)
    public PostListResponse getPostList(Long cursor, int size) {
        // 페이징 조건 커서 위치부터 사이즈+1개 가져오기 커서기반이라 처음은 0
        // 커서기준부터 사이즈 값만큼 가져오는것이기 떄문
        Pageable pageable = PageRequest.of(0, size + 1);

        List<Post> posts;

        if(cursor == null) {
            // pageable에 적힌 갯수만큼 id값 내림차순으로 가져오기
            posts = postRepository.findAllByOrderByIdDesc(pageable).getContent();
        } else {
            // 커서 위치에서 pageable에 적힌 갯수만큼 id값 내림차순으로 가져오기
            posts = postRepository.findByIdLessThanOrderByIdDesc(cursor, pageable).getContent();
        }

        // 보낸 사이즈보다 조회한 사이즈가 더 크면 다음 페이지 있는거임 위에서 + 1해서
        boolean hasNext = posts.size() > size;

        // 11번째 게시글은 버림
        if (hasNext) {
            posts = posts.subList(0, size);
        }

        // 마지막 게시글에 커서
        Long nextCursor = hasNext && !posts.isEmpty()
                ? posts.getLast().getId()
                : null;

        // ⭐️게시글 최적화 쿼리 작성하기
        List<PostListInfo> postListInfos = posts.stream()
                .map(post -> new PostListInfo(
                        post.getId(),
                        post.getTitle(),
                        post.getLikesCount(),
                        post.getCommentsCount(),
                        post.getViewsCount(),
                        post.getCreatedAt(),
                        post.getUser().getId(),
                        post.getUser().getNickname(),
                        post.getUser().getProfileImage()
                ))
                .toList();

        return new PostListResponse(
                postListInfos,
                new PageInfo(hasNext, nextCursor)
        );
    }

    // 게시글 상세 조회
    @Override
    @Transactional() //readOnly = true 제거 이유 뷰 카운트 증가
                     // 나중에 리포지토리에 커스텀 쿼리로 동시성 제어 및 이 메소드는 readOnly만 하게 하기
    public PostDetailResponse getPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
        boolean liked = postLikeRepository.existsByUserIdAndPostId(userId, postId);
        post.increaseViewsCount(); // 동시성 문제 확인해야함
        return PostDetailResponse.from(post, liked);
    }

    // 게시글 수정
    @Override
    @Transactional
    public PostUpdateResponse updatePost(Long userId, Long postId, PostUpdateRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("게시글 작성자만 수정할 수 있습니다.");
        }

        if (!request.hasUpdateValue()) {
            throw new IllegalArgumentException("수정할 값을 입력해주세요.");
        }


        post.update(request.title(), request.content());

        return PostUpdateResponse.from(post);
    }

    // 게시글 삭제
    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        if (!userRepository.existsById(userId)) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("게시글 작성자만 삭제할 수 있습니다.");
        }
        postRepository.delete(post);
    }
}
