package com.sprint.otboo.clothing.exception;

/**
 * 의상 생성 요청 검증 실패 시 발생하는 예외
 */
public class ClothesValidationException extends RuntimeException {

  /**
   * 생성자
   *
   * @param message 예외 메시지
   */
    public ClothesValidationException(String message) {
        super(message);
    }
}
