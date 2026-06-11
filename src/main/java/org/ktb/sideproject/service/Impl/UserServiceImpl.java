package org.ktb.sideproject.service.Impl;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.dto.auth.req.SignupRequest;
import org.ktb.sideproject.dto.user.res.UserInfo;
import org.ktb.sideproject.dto.user.req.NicknameUpdateRequest;
import org.ktb.sideproject.dto.user.req.PasswordUpdateRequest;
import org.ktb.sideproject.dto.user.req.ProfileImageUpdateRequest;
import org.ktb.sideproject.entity.Comment;
import org.ktb.sideproject.entity.ImageStatus;
import org.ktb.sideproject.entity.Post;
import org.ktb.sideproject.entity.PostImage;
import org.ktb.sideproject.entity.PostLike;
import org.ktb.sideproject.entity.ProfileImage;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.error.CustomException;
import org.ktb.sideproject.error.ErrorCode;
import org.ktb.sideproject.repository.CommentRepository;
import org.ktb.sideproject.repository.PostLikeRepository;
import org.ktb.sideproject.repository.PostRepository;
import org.ktb.sideproject.repository.ProfileImageRepository;
import org.ktb.sideproject.repository.RefreshTokenRepository;
import org.ktb.sideproject.repository.UserRepository;
import org.ktb.sideproject.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final ProfileImageRepository profileImageRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    @Transactional
    public void signup(SignupRequest signupRequest) {

        if(!isEmailAvailable(signupRequest.email())){
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if(!isNicknameAvailable(signupRequest.nickname())){
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }


        User user = User.builder()
                .email(signupRequest.email())
                .password(passwordEncoder.encode(signupRequest.password()))
                .nickname(signupRequest.nickname())
                .build();

        User savedUser = userRepository.save(user);
        attachProfileImage(savedUser, signupRequest.profileImage());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailAvailable(String email) {
        return !userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new UserInfo(user.getId(), user.getEmail(), user.getNickname(), getProfileImageUrl(user.getId()));
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if (!request.password().equals(request.passwordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
        }

        user.changePassword(passwordEncoder.encode(request.password()));
    }

    @Override
    @Transactional
    public UserInfo updateNickname(Long userId, NicknameUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (userRepository.existsByNicknameAndIdNot(request.nickname(), userId)) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        user.changeNickname(request.nickname());

        return new UserInfo(user.getId(), user.getEmail(), user.getNickname(), getProfileImageUrl(user.getId()));
    }

    @Override
    @Transactional
    public UserInfo updateProfileImage(Long userId, ProfileImageUpdateRequest request) {
        User user = userRepository.findById(userId).
                orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        attachProfileImage(user, request.profileImage());
        return new  UserInfo(user.getId(), user.getEmail(), user.getNickname(), getProfileImageUrl(user.getId()));
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Post> userPosts = postRepository.findByUserId(userId);
        Set<Long> userPostIds = userPosts.stream()
                .map(Post::getId)
                .collect(Collectors.toSet());

        List<String> postImageStorageKeys = userPosts.stream()
                .flatMap(post -> post.getImages().stream())
                .map(PostImage::getStorageKey)
                .toList();

        deleteUserLikes(userId, userPostIds);
        deleteUserComments(userId, userPostIds);
        refreshTokenRepository.deleteByUserId(userId);

        profileImageRepository.findByUserIdAndStatus(userId, ImageStatus.SAVED)
                .ifPresent(profileImage -> {
                    profileImageRepository.delete(profileImage);
                    deleteImageFile(profileImage.getStorageKey());
                });

        if (!userPostIds.isEmpty()) {
            List<Long> postIds = userPostIds.stream().toList();
            postLikeRepository.deleteByPostIdIn(postIds);
            commentRepository.deleteByPostIdIn(postIds);
        }

        postRepository.deleteAll(userPosts);
        deleteImageFiles(postImageStorageKeys);
        userRepository.delete(user);
    }

    private void deleteUserLikes(Long userId, Set<Long> userPostIds) {
        List<PostLike> likes = postLikeRepository.findByUserId(userId);
        List<PostLike> likesOnOtherPosts = likes.stream()
                .filter(postLike -> !userPostIds.contains(postLike.getPost().getId()))
                .toList();

        likesOnOtherPosts.forEach(postLike -> postLike.getPost().decreaseLikesCount());
        postLikeRepository.deleteAll(likesOnOtherPosts);
    }

    private void deleteUserComments(Long userId, Set<Long> userPostIds) {
        List<Comment> comments = commentRepository.findByUserId(userId);
        List<Comment> commentsOnOtherPosts = comments.stream()
                .filter(comment -> !userPostIds.contains(comment.getPost().getId()))
                .toList();

        commentsOnOtherPosts.forEach(comment -> comment.getPost().decreaseCommentsCount());
        commentRepository.deleteAll(commentsOnOtherPosts);
    }

    private void attachProfileImage(User user, String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return;
        }

        ProfileImage profileImage = profileImageRepository.findByImageUrlAndStatus(profileImageUrl, ImageStatus.PENDING)
                .orElseThrow(() -> new CustomException(ErrorCode.PROFILE_IMAGE_NOT_AVAILABLE));

        Optional<ProfileImage> previousImage = profileImageRepository.findByUserIdAndStatus(user.getId(), ImageStatus.SAVED);
        previousImage.ifPresent(profileImageRepository::delete);
        profileImageRepository.flush();

        profileImage.attachTo(user);

        previousImage
                .map(ProfileImage::getStorageKey)
                .ifPresent(this::deleteImageFile);
    }

    private String getProfileImageUrl(Long userId) {
        return profileImageRepository.findByUserIdAndStatus(userId, ImageStatus.SAVED)
                .map(ProfileImage::getImageUrl)
                .orElse("default-profile.png");
    }

    private void deleteImageFile(String storageKey) {
        Path imagePath = Path.of(uploadDir).resolve(storageKey).normalize();

        try {
            Files.deleteIfExists(imagePath);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.IMAGE_DELETE_FAILED);
        }
    }

    private void deleteImageFiles(List<String> storageKeys) {
        for (String storageKey : storageKeys) {
            deleteImageFile(storageKey);
        }
    }
}
