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
    WEATHER_BAD_COORDINATE(HttpStatus.BAD_REQUEST, "잘못된 좌표 값입니다."),
    WEATHER_GRID_CONVERSION_FAILED(HttpStatus.BAD_REQUEST, "격자 변환에 실패했습니다."),
    REQUIRED_COOKIE_MISSING(HttpStatus.BAD_REQUEST, "필수 쿠키가 요청에 포함되지 않았습니다."),
    FOLLOW_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신을 팔로우할 수 없습니다."),
    INVALID_PAGING_LIMIT(HttpStatus.BAD_REQUEST, "limit 값이 올바르지 않습니다."),
    INVALID_SORT_BY(HttpStatus.BAD_REQUEST, "허용되지 않는 정렬 컬럼입니다."),
    INVALID_SORT_DIRECTION(HttpStatus.BAD_REQUEST, "허용되지 않는 정렬 방향입니다."),
    WEATHER_DATA_INCOMPLETE(HttpStatus.BAD_REQUEST, "날씨 데이터가 불완전합니다."),
    INVALID_CURSOR_FORMAT(org.springframework.http.HttpStatus.BAD_REQUEST, "cursor 형식이 잘못되었습니다(ISO-8601 필요)."),
    INVALID_CURSOR_PAIR(HttpStatus.BAD_REQUEST, "cursor와 idAfter는 함께 제공해야 합니다."),
    SELF_DM_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "자기 자신과의 대화는 허용되지 않습니다."),

    // 401 UNAUTHORIZED
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 요청입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // 403 FORBIDDEN
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    FEED_DENIED(HttpStatus.FORBIDDEN, "피드에 대한 권한이 없습니다."),
    FOLLOW_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 팔로우에 대한 권한이 없습니다."),

    // 404 NOT FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자 프로필을 찾을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    WEATHER_LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 위치의 날씨 정보를 찾을 수 없습니다."),
    WEATHER_NOT_FOUND(HttpStatus.NOT_FOUND, "날씨 정보를 찾을 수 없습니다."),
    CLOTHES_NOT_FOUND(HttpStatus.NOT_FOUND, "의상 정보를 찾을 수 없습니다."),
    USER_CLOTHES_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자의 일부 의상을 찾을 수 없습니다"),
    FEED_NOT_FOUND(HttpStatus.NOT_FOUND, "피드를 찾을 수 없습니다."),
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND,"알림을 찾을 수 없습니다."),
    FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "팔로우 정보를 찾을 수 없습니다."),

    // 409 CONFLICT
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용 중인 사용자명입니다."),
    FOLLOW_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 팔로우 중입니다."),

    // 415 UNSUPPORTED_MEDIA_TYPE
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 미디어 타입입니다."),

    // 423 LOCKED
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "잠긴 계정입니다."),

    // 429 Too Many Requests
    WEATHER_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "외부 제공자 쿼터가 초과되었습니다."),

    // 500 INTERNAL SERVER ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 처리 중 오류가 발생했습니다."),

    // 502 Bad Gateway
    WEATHER_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "외부 날씨/위치 제공자 오류가 발생했습니다."),

    // 503 Service Unavailable
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "현재 서비스를 사용할 수 없습니다."),
    MAIL_SEND_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "메일 전송에 실패했습니다."),

    // 504 Gateway Timeout
    WEATHER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "외부 제공자 응답 지연이 발생했습니다."),

    TOKEN_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "JWT 토큰 생성에 실패했습니다.");


    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
