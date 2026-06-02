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

        return PostDetailResponse.from(currentPost);
    }

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

        List<PostListInfo> postListInfos = posts.stream()
                .map(post -> new PostListInfo(
                        post.getId(),
                        post.getTitle(),
                        post.getLikesCount(),
                        post.getCommentsCount(),
                        post.getViewsCount(),
                        post.getCreatedAt()
                ))
                .toList();

        return new PostListResponse(
                postListInfos,
                new PageInfo(hasNext, nextCursor)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PostDetailResponse getPost(Long postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        return PostDetailResponse.from(post);
    }

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

    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        if (!post.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("게시글 작성자만 삭제할 수 있습니다.");
        }
        postRepository.delete(post);
    }
}
