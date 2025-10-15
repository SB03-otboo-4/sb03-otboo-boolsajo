package com.sprint.otboo.clothing.exception;

/**
 * 의상 정보 추출 실패 시 발생하는 커스텀 런타임 예외
 */
public class ClothesExtractionException extends RuntimeException {

    /**
     * 메시지만 있는 생성자
     *
     * @param message 예외 메시지
     */
    public ClothesExtractionException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인 예외를 포함한 생성자
     *
     * @param message 예외 메시지
     * @param cause 원인 예외
     */
    public ClothesExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}