package com.sprint.otboo.common.exception;

import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import com.sprint.otboo.common.dto.ErrorResponse;
import com.sprint.otboo.common.exception.auth.MailSendFailedException;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * 전역 예외 처리 클래스
 *
 * <p>애플리케이션 전역에서 발생하는 예외를 처리하며, HTTP 상태 코드와
 * 오류 메시지를 클라이언트에 일관되게 전달합니다.</p>
 *
 * <ul>
 *   <li>커스텀 예외 (CustomException)</li>
 *   <li>잘못된 인수 (IllegalArgumentException)</li>
 *   <li>JSON 파싱 오류 (HttpMessageNotReadableException)</li>
 *   <li>유효성 검사 실패 (MethodArgumentNotValidException)</li>
 *   <li>권한 거부 (AuthorizationDeniedException)</li>
 *   <li>Enum 타입 변환 실패 (MethodArgumentTypeMismatchException)</li>
 *   <li>기타 예외 (Exception)</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 커스텀 예외 처리
     *
     * @param e 발생한 CustomException
     * @return 예외 코드 및 메시지를 담은 ResponseEntity
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.error("커스텀 예외 발생: code={}, status={}, message={}",
            errorCode.name(), errorCode.getStatus().value(), e.getMessage());

        ErrorResponse body = new ErrorResponse(e);
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(body);
    }

    /**
     * 잘못된 인수 예외 처리
     *
     * @param e 발생한 IllegalArgumentException
     * @return 400 상태 코드와 오류 메시지
     */
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

    /**
     * JSON 파싱 오류 처리
     *
     * @param e 발생한 HttpMessageNotReadableException
     * @return 400 상태 코드와 "잘못된 JSON 형식" 메시지
     */
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

    /**
     * 요청 유효성 검사 실패 처리
     *
     * @param ex 발생한 MethodArgumentNotValidException
     * @return 400 상태 코드와 필드별 검증 메시지
     */
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

    /**
     * 권한 거부 예외 처리
     *
     * @param ex 발생한 AuthorizationDeniedException
     * @return 403 상태 코드와 오류 메시지
     */
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

    @ExceptionHandler(MissingRequestCookieException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestCookieException(MissingRequestCookieException ex) {
        log.warn("필수 쿠키 누락: {}", ex.getMessage());

        ErrorCode errorCode = ErrorCode.REQUIRED_COOKIE_MISSING;

        String message = String.format("필수 쿠키 '%s'가 요청에 포함되지 않았습니다.", ex.getCookieName());

        ErrorResponse body = new ErrorResponse(
            Instant.now(),
            errorCode.name(),
            message,
            new HashMap<>(),
            ex.getClass().getSimpleName(),
            errorCode.getStatus().value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 예상치 못한 예외 처리
     *
     * @param e 발생한 Exception
     * @return 500 상태 코드와 일반 오류 메시지
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("예상치 못한 오류 발생: {}", e.getMessage(), e);

        ErrorResponse body = new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    /**
     * 요청 파라미터 바인딩 실패 처리
     *
     * <p>Enum 변환 실패 등 {@link MethodArgumentTypeMismatchException}이 발생했을 때
     * HTTP 400(Bad Request)로 응답하며, 프론트에 커스텀 메시지와 허용 값 목록을 제공합니다.</p>
     *
     * @param ex 발생한 MethodArgumentTypeMismatchException
     * @return 400 상태 코드와 오류 정보를 담은 Map
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> body = new HashMap<>();
        if (ex.getRequiredType() != null && ex.getRequiredType().isEnum()) {
            body.put("error", "유효하지 않은 의상 타입입니다.");
            body.put("allowedValues", ex.getRequiredType().getEnumConstants());
        } else {
            body.put("error", "잘못된 요청 파라미터입니다.");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        log.error("필수 요청 파라미터 누락: name={}, type={}", e.getParameterName(), e.getParameterType());

        Map<String, Object> details = new HashMap<>();
        details.put("parameterName", e.getParameterName());
        details.put("parameterType", e.getParameterType());

        ErrorResponse body = new ErrorResponse(
            Instant.now(),
            "BAD_REQUEST",
            "필수 요청 파라미터가 누락되었습니다.",
            details,
            e.getClass().getSimpleName(),
            HttpStatus.BAD_REQUEST.value()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        log.error("요청 파라미터 제약 위반: {}", ex.getMessage());
        Map<String, Object> details = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
            details.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        ErrorResponse body = new ErrorResponse(
            Instant.now(),
            ErrorCode.VALIDATION_FAILED.name(),
            ErrorCode.VALIDATION_FAILED.getMessage(),
            details,
            ex.getClass().getSimpleName(),
            ErrorCode.VALIDATION_FAILED.getStatus().value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("권한 없음: {}", ex.getMessage());
        Map<String, Object> details = new HashMap<>();
        ErrorResponse body = new ErrorResponse(
            Instant.now(),
            ErrorCode.ACCESS_DENIED.name(),
            ex.getMessage(),
            details,
            ex.getClass().getSimpleName(),
            HttpStatus.FORBIDDEN.value()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MailSendFailedException.class)
    public ResponseEntity<ErrorResponse> handleMailSendFailedException(MailSendFailedException ex) {
        log.error("메일 전송 서비스 오류 발생: {}", ex.getMessage(), ex.getCause());

        ErrorResponse body = new ErrorResponse(ex);

        return ResponseEntity.status(ex.getErrorCode().getStatus()).body(body);
    }

    @ExceptionHandler(ClothesExtractionException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleClothesExtractionException(ClothesExtractionException ex) {
        return Map.of("message", ex.getMessage());
    }

}
