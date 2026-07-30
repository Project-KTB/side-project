package org.ktb.sideproject.service.Impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class RefreshTokenHasher {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final SecretKeySpec secretKeySpec;

    public RefreshTokenHasher(@Value("${refresh-token.hash-secret}") String refreshTokenHashSecret) {
        this.secretKeySpec = new SecretKeySpec(refreshTokenHashSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
    }

    public String hash(String refreshToken) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKeySpec);
            return HexFormat.of().formatHex(mac.doFinal(refreshToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to hash refresh token", e);
        }
    }
}
