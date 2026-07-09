package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.post.PageInfo;
import org.ktb.sideproject.dto.post.PostListInfo;
import org.ktb.sideproject.dto.post.req.PostCreateRequest;
import org.ktb.sideproject.dto.post.req.PostUpdateRequest;
import org.ktb.sideproject.dto.post.res.PostListResponse;
import org.ktb.sideproject.dto.post.res.PostDetailResponse;
import org.ktb.sideproject.dto.post.res.PostUpdateResponse;
import org.ktb.sideproject.entity.ImageStatus;
import org.ktb.sideproject.entity.Post;
import org.ktb.sideproject.entity.PostImage;
import org.ktb.sideproject.entity.ProfileImage;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.repository.CommentRepository;
import org.ktb.sideproject.repository.PostLikeRepository;
import org.ktb.sideproject.repository.PostImageRepository;
import org.ktb.sideproject.repository.PostRepository;
import org.ktb.sideproject.repository.ProfileImageRepository;
import org.ktb.sideproject.repository.UserRepository;
import org.ktb.sideproject.service.PostService;
import org.ktb.sideproject.service.storage.ImageStorageService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostImageRepository postImageRepository;
    private final ProfileImageRepository profileImageRepository;
    private final CommentRepository commentRepository;
    private final ImageStorageService imageStorageService;

    // 게시글 생성
    @Override
    @Transactional
    public PostDetailResponse createPost(Long userId, PostCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.builder()
                .content(request.content())
                .title(request.title())
                .user(user)
                .build();

        addImages(post, request.imageUrls());

        Post currentPost = postRepository.save(post);

        return PostDetailResponse.from(currentPost, false, getProfileImageUrl(user.getId()));
    }

    // 게시글 목록 조회
    @Override
    @Transactional(readOnly = true)
    public PostListResponse getPostList(String keyword, Long cursor, int size) {
        // 페이징 조건 커서 위치부터 사이즈+1개 가져오기 커서기반이라 처음은 0
        // 커서기준부터 사이즈 값만큼 가져오는것이기 떄문
        Pageable pageable = PageRequest.of(0, size + 1);
        String normalizedKeyword = normalizeKeyword(keyword);

        List<Post> posts = postRepository.searchByKeywordOrderByIdDesc(
                normalizedKeyword,
                cursor,
                pageable
        ).getContent();

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

        Map<Long, String> profileImageUrlByUserId = getProfileImageUrlMap(posts);

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
                        profileImageUrlByUserId.getOrDefault(post.getUser().getId(), "default-profile.png")
                ))
                .toList();

        return new PostListResponse(
                postListInfos,
                new PageInfo(hasNext, nextCursor)
        );
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim();
    }

    // 게시글 상세 조회
    @Override
    @Transactional() //readOnly = true 제거 이유 뷰 카운트 증가
                     // 나중에 리포지토리에 커스텀 쿼리로 동시성 제어 및 이 메소드는 readOnly만 하게 하기
    public PostDetailResponse getPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        boolean liked = postLikeRepository.existsByUserIdAndPostId(userId, postId);
        post.increaseViewsCount(); // 동시성 문제 확인해야함
        return PostDetailResponse.from(post, liked, getProfileImageUrl(post.getUser().getId()));
    }

    // 게시글 수정
    @Override
    @Transactional
    public PostUpdateResponse updatePost(Long userId, Long postId, PostUpdateRequest request) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "게시글 작성자만 수정할 수 있습니다.");
        }

        if (!request.hasUpdateValue()) {
            throw new CustomException(ErrorCode.POST_UPDATE_VALUE_REQUIRED);
        }


        post.update(request.title(), request.content());
        updateImages(post, request.imageUrls());

        return PostUpdateResponse.from(post);
    }

    // 게시글 삭제
    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ACCESS_DENIED, "게시글 작성자만 삭제할 수 있습니다.");
        }
        List<String> storageKeys = post.getImages()
                .stream()
                .map(PostImage::getStorageKey)
                .toList();

        postLikeRepository.deleteByPostIdIn(List.of(postId));
        commentRepository.deleteByPostIdIn(List.of(postId));
        postRepository.delete(post);
        deleteImageFiles(storageKeys);
    }

    private void addImages(Post post, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        List<String> validImageUrls = normalizeImageUrls(imageUrls);
        List<PostImage> images = postImageRepository.findByImageUrlInAndStatus(validImageUrls, ImageStatus.PENDING);

        if (images.size() != validImageUrls.size()) {
            throw new CustomException(ErrorCode.POST_IMAGE_NOT_AVAILABLE);
        }

        for (PostImage image : images) {
            post.addImage(image);
        }
    }

    private void updateImages(Post post, List<String> imageUrls) {
        if (imageUrls == null) {
            return;
        }

        List<String> validImageUrls = normalizeImageUrls(imageUrls);
        Set<String> finalImageUrlSet = new LinkedHashSet<>(validImageUrls);
        List<PostImage> currentImages = new ArrayList<>(post.getImages());
        Map<String, PostImage> currentImageByUrl = currentImages.stream()
                .collect(Collectors.toMap(
                        PostImage::getImageUrl,
                        image -> image
                ));

        List<String> newImageUrls = validImageUrls.stream()
                .filter(imageUrl -> !currentImageByUrl.containsKey(imageUrl))
                .toList();
        List<PostImage> newImages = newImageUrls.isEmpty()
                ? List.of()
                : postImageRepository.findByImageUrlInAndStatus(newImageUrls, ImageStatus.PENDING);

        if (newImages.size() != newImageUrls.size()) {
            throw new CustomException(ErrorCode.POST_IMAGE_NOT_AVAILABLE);
        }

        Map<String, PostImage> newImageByUrl = newImages.stream()
                .collect(Collectors.toMap(
                        PostImage::getImageUrl,
                        image -> image
                ));

        List<String> removedStorageKeys = currentImages.stream()
                .filter(image -> !finalImageUrlSet.contains(image.getImageUrl()))
                .map(PostImage::getStorageKey)
                .toList();

        currentImages.stream()
                .filter(image -> !finalImageUrlSet.contains(image.getImageUrl()))
                .forEach(post::removeImage);

        for (String imageUrl : newImageUrls) {
            post.addImage(newImageByUrl.get(imageUrl));
        }

        deleteImageFiles(removedStorageKeys);
    }

    private List<String> normalizeImageUrls(List<String> imageUrls) {
        Set<String> uniqueImageUrls = new LinkedHashSet<>();// 중복제거, 순서 유지

        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.isBlank()) {
                continue;
            }

            validateImageUrl(imageUrl); // 이미지 URL 검증
            uniqueImageUrls.add(imageUrl);
        }

        return new ArrayList<>(uniqueImageUrls);
    }

    private void validateImageUrl(String imageUrl) {
        String prefix = imageStorageService.imageUrlPrefix();

        if (!imageUrl.startsWith(prefix + "/")) {
            throw new CustomException(ErrorCode.INVALID_IMAGE_URL);
        }
    }

    private String getProfileImageUrl(Long userId) {
        return profileImageRepository.findByUserIdAndStatus(userId, ImageStatus.SAVED)
                .map(ProfileImage::getImageUrl)
                .orElse("default-profile.png");
    }

    private Map<Long, String> getProfileImageUrlMap(List<Post> posts) {
        List<Long> userIds = posts.stream()
                .map(post -> post.getUser().getId())
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

    private void deleteImageFiles(List<String> storageKeys) {
        for (String storageKey : storageKeys) {
            imageStorageService.delete(storageKey);
        }
    }
}
