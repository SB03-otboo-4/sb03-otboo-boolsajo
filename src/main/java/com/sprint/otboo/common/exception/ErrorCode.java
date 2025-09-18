package com.sprint.otboo.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 400 BAD REQUEST
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력 값입니다."),
    SAME_PASSWORD(HttpStatus.BAD_REQUEST,"새로운 비밀번호가 기존 비밀번호와 동일합니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값 검증에 실패했습니다."),

    // 401 UNAUTHORIZED
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 요청입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // 403 FORBIDDEN
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 404 NOT FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    WEATHER_NOT_FOUND(HttpStatus.NOT_FOUND, "날씨 정보를 찾을 수 없습니다."),
    CLOTHES_NOT_FOUND(HttpStatus.NOT_FOUND, "의상 정보를 찾을 수 없습니다."),
    USER_CLOTHES_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자의 일부 의상을 찾을 수 없습니다"),
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "피드를 찾을 수 없습니다."),

    // 409 CONFLICT
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 사용자명입니다."),

    // 415 UNSUPPORTED_MEDIA_TYPE
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),

    // 423 LOCKED
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "잠긴 계정입니다."),

    // 500 INTERNAL SERVER ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "현재 서비스를 사용할 수 없습니다."),
    TOKEN_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "JWT 토큰 생성에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
