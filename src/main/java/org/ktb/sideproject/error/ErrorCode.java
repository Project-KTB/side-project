package org.ktb.sideproject.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버에 예상치 못한 문제가 발생했습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력 형식이 잘못되었습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "INVALID_TYPE_VALUE", "잘못된 요청입니다. 올바른 값을 입력해주세요."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "해당 리소스에 접근할 권한이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인 후 이용할 수 있습니다."),

    // Auth
    LOGIN_FAILED(HttpStatus.BAD_REQUEST, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "인증 토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 인증 정보입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND", "저장된 Refresh Token이 없습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_MISMATCH", "RefreshToken이 일치하지 않습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "해당 회원 정보를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "중복된 이메일입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "DUPLICATE_NICKNAME", "중복된 닉네임입니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_CONFIRM_MISMATCH", "비밀번호 확인과 다릅니다."),
    PROFILE_IMAGE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "PROFILE_IMAGE_NOT_AVAILABLE", "업로드되지 않았거나 이미 사용된 프로필 이미지입니다."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "존재하지 않거나 삭제된 게시글입니다."),
    INVALID_PAGINATION_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_PAGINATION_PARAMETER", "페이징 요청 값이 올바르지 않습니다."),
    POST_UPDATE_VALUE_REQUIRED(HttpStatus.BAD_REQUEST, "POST_UPDATE_VALUE_REQUIRED", "수정할 값을 입력해주세요."),
    POST_IMAGE_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "POST_IMAGE_NOT_AVAILABLE", "업로드되지 않았거나 이미 사용된 이미지가 포함되어 있습니다."),
    INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_URL", "지원하지 않는 이미지 URL입니다."),

    // Comment
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND", "존재하지 않거나 이미 삭제된 댓글입니다."),
    COMMENT_UPDATE_VALUE_REQUIRED(HttpStatus.BAD_REQUEST, "COMMENT_UPDATE_VALUE_REQUIRED", "수정할 값을 입력해주세요."),

    // Like
    ALREADY_LIKED_POST(HttpStatus.CONFLICT, "ALREADY_LIKED_POST", "이미 좋아요를 누른 게시글입니다."),
    POST_LIKE_NOT_FOUND(HttpStatus.BAD_REQUEST, "POST_LIKE_NOT_FOUND", "좋아요를 누르지 않은 게시글입니다."),

    // Image
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "이미지를 찾을 수 없습니다."),
    IMAGE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "IMAGE_ACCESS_DENIED", "이미지에 접근할 권한이 없습니다."),
    IMAGE_FILE_REQUIRED(HttpStatus.BAD_REQUEST, "IMAGE_FILE_REQUIRED", "이미지 파일을 첨부해주세요."),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "IMAGE_SIZE_EXCEEDED", "이미지 파일은 5MB 이하만 업로드할 수 있습니다."),
    INVALID_IMAGE_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_CONTENT_TYPE", "jpg, png, gif, webp 이미지만 업로드할 수 있습니다."),
    IMAGE_EXTENSION_REQUIRED(HttpStatus.BAD_REQUEST, "IMAGE_EXTENSION_REQUIRED", "이미지 파일 확장자가 필요합니다."),
    INVALID_IMAGE_EXTENSION(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_EXTENSION", "지원하지 않는 이미지 확장자입니다."),
    IMAGE_SAVE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_SAVE_FAILED", "이미지 저장에 실패했습니다."),
    IMAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_DELETE_FAILED", "이미지 파일 삭제에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}
