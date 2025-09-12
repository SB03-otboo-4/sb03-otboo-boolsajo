package com.sprint.otboo.common.dto;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public record ErrorResponse(
    Instant timestamp,
    String code,
    String message,
    Map<String, Object> details,
    String exceptionType,
    int status
) {

    // ErrorCode 기반 생성자
    public ErrorResponse(ErrorCode errorCode) {
        this(
            Instant.now(),
            errorCode.name(),
            errorCode.getMessage(),
            new HashMap<>(),
            errorCode.getClass().getSimpleName(),
            errorCode.getStatus().value()
        );
    }

    // CustomException 기반 생성자
    public ErrorResponse(CustomException exception) {
        this(
            Instant.now(),
            exception.getErrorCode().name(),
            exception.getMessage(),
            new HashMap<>(exception.getDetails()),
            exception.getClass().getSimpleName(),
            exception.getErrorCode().getStatus().value()
        );
    }

    // 일반 Exception 기반 생성자
    public ErrorResponse(Exception exception, int status) {
        this(
            Instant.now(),
            exception.getClass().getSimpleName(),
            exception.getMessage(),
            new HashMap<>(),
            exception.getClass().getSimpleName(),
            status
        );
    }
}
