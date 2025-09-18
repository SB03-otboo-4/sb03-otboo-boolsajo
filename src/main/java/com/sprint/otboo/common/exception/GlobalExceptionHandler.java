package com.sprint.otboo.common.exception;

import com.sprint.otboo.common.dto.ErrorResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("커스텀 예외 발생: code={}, status={}, message={}",
            errorCode.name(), errorCode.getStatus().value(), e.getMessage());

        ErrorResponse body = new ErrorResponse(e);
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("잘못된 인수 예외 발생: {}", e.getMessage());

        ErrorResponse body = new ErrorResponse(
            Instant.now(),
            "BAD_REQUEST",
            e.getMessage(),
            new HashMap<>(),
            e.getClass().getSimpleName(),
            HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("HTTP 메시지 읽기 오류 발생: {}", e.getMessage());

        ErrorResponse body = new ErrorResponse(
            Instant.now(),
            "BAD_REQUEST",
            "잘못된 JSON 형식입니다.",
            new HashMap<>(),
            e.getClass().getSimpleName(),
            HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
        MethodArgumentNotValidException ex) {
        log.error("요청 유효성 검사 실패: {}", ex.getMessage());

        Map<String, Object> validationErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError fieldError) {
                validationErrors.put(fieldError.getField(), error.getDefaultMessage());
            } else {
                validationErrors.put(error.getObjectName(), error.getDefaultMessage());
            }
        });

        ErrorResponse body = new ErrorResponse(
            Instant.now(),
            ErrorCode.VALIDATION_FAILED.name(),
            ErrorCode.VALIDATION_FAILED.getMessage(),
            validationErrors,
            ex.getClass().getSimpleName(),
            ErrorCode.VALIDATION_FAILED.getStatus().value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(
        AuthorizationDeniedException ex) {
        log.error("권한 거부 오류 발생: {}", ex.getMessage());

        ErrorResponse body = new ErrorResponse(
            Instant.now(),
            ErrorCode.ACCESS_DENIED.name(),
            ErrorCode.ACCESS_DENIED.getMessage(),
            new HashMap<>(),
            ex.getClass().getSimpleName(),
            ErrorCode.ACCESS_DENIED.getStatus().value()
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex) {

        ErrorCode errorCode = ErrorCode.UNSUPPORTED_MEDIA_TYPE;

        String detailedMessage = String.format("'%s'은(는) 지원하지 않는 미디어 타입입니다. 지원하는 타입: '%s'",
            ex.getContentType(), ex.getSupportedMediaTypes().get(0));

        ErrorResponse body = new ErrorResponse(
            Instant.now(),
            errorCode.name(),
            detailedMessage,
            new HashMap<>(),
            ex.getClass().getSimpleName(),
            errorCode.getStatus().value()
        );

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);

        ErrorResponse body = new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
