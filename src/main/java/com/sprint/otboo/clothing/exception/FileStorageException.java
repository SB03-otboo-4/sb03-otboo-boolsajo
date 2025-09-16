package com.sprint.otboo.clothing.exception;

/**
 * 파일 스토리지 관련 예외
 *
 * <p>파일 업로드/삭제 과정에서 발생하는 모든 예외를 통일적으로 처리하기 위해 사용
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
