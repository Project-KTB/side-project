package org.ktb.sideproject.repository;

import org.ktb.sideproject.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByRefreshTokenHash(String refreshTokenHash);

    void deleteByRefreshTokenHash(String refreshTokenHash);

    void deleteByUserId(Long userId);
}
