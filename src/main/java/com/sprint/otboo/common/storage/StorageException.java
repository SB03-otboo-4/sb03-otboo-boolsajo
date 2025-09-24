package com.sprint.otboo.common.storage;

/**
 * 파일 저장/삭제 과정에서 발생하는 I/O 예외를 싸고 있는 RuntimeException
 * */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
