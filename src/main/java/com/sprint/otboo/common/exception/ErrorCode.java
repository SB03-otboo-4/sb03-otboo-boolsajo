package com.sprint.otboo.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 400 BAD REQUEST
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력 값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "요청 값 검증에 실패했습니다."),
    WEATHER_BAD_COORDINATE(HttpStatus.BAD_REQUEST, "잘못된 좌표 값입니다."),
    WEATHER_GRID_CONVERSION_FAILED(HttpStatus.BAD_REQUEST, "격자 변환에 실패했습니다."),

    // 401 UNAUTHORIZED
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 요청입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."),

    // 403 FORBIDDEN
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),

    // 404 NOT FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다."),
    WEATHER_LOCATION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 위치의 날씨 정보를 찾을 수 없습니다."),

    // 409 CONFLICT
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "이미 존재하는 리소스입니다."),

    // 429 Too Many Requests
    WEATHER_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "외부 제공자 쿼터가 초과되었습니다."),

    // 500 INTERNAL SERVER ERROR
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "현재 서비스를 사용할 수 없습니다."),

    // 502 Bad Gateway
    WEATHER_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "외부 날씨/위치 제공자 오류가 발생했습니다."),

    // 504 Gateway Timeout
    WEATHER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "외부 제공자 응답 지연이 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
