package org.ktb.sideproject.config;

import lombok.RequiredArgsConstructor;
import org.ktb.sideproject.entity.Comment;
import org.ktb.sideproject.entity.Post;
import org.ktb.sideproject.entity.PostLike;
import org.ktb.sideproject.entity.User;
import org.ktb.sideproject.repository.CommentRepository;
import org.ktb.sideproject.repository.PostLikeRepository;
import org.ktb.sideproject.repository.PostRepository;
import org.ktb.sideproject.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class SeedConfig {

    private static final String SEED_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    @Bean
    public CommandLineRunner seedData() {
        return args -> transactionTemplate.executeWithoutResult(status -> {
            if (userRepository.existsByEmail("seed1@example.com")) {
                return;
            }

            User user1 = createUser("seed1@example.com", "씨드유저1");
            User user2 = createUser("seed2@example.com", "씨드유저2");
            User user3 = createUser("seed3@example.com", "씨드유저3");

            Post post1 = createPost(user1, "첫 번째 게시글", "첫 번째 게시글 본문입니다.");
            Post post2 = createPost(user2, "두 번째 게시글", "두 번째 게시글 본문입니다.");
            Post post3 = createPost(user3, "세 번째 게시글", "세 번째 게시글 본문입니다.");
            Post post4 = createPost(user1, "네 번째 게시글", "네 번째 게시글 본문입니다.");
            Post post5 = createPost(user2, "다섯 번째 게시글", "다섯 번째 게시글 본문입니다.");

            createComment(user2, post1, "첫 번째 게시글 댓글입니다.");
            createComment(user3, post1, "저도 댓글 남깁니다.");
            createComment(user1, post2, "두 번째 게시글에 댓글입니다.");
            createComment(user3, post4, "네 번째 게시글 좋아요.");

            createLike(user2, post1);
            createLike(user3, post1);
            createLike(user1, post2);
            createLike(user1, post3);
            createLike(user3, post5);
        });
    }

    private User createUser(String email, String nickname) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(SEED_PASSWORD))
                .nickname(nickname)
                .profileImage("default-profile.png")
                .build();

        return userRepository.save(user);
    }

    private Post createPost(User user, String title, String content) {
        Post post = Post.builder()
                .title(title)
                .content(content)
                .user(user)
                .build();

        return postRepository.save(post);
    }

    private void createComment(User user, Post post, String content) {
        Comment comment = Comment.builder()
                .user(user)
                .post(post)
                .content(content)
                .build();

        commentRepository.save(comment);
        post.increaseCommentsCount();
    }

    private void createLike(User user, Post post) {
        PostLike postLike = PostLike.builder()
                .user(user)
                .post(post)
                .build();

        postLikeRepository.save(postLike);
        post.increaseLikesCount();
    }
}
