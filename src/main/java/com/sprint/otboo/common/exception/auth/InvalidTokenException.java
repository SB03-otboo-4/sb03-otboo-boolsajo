package com.sprint.otboo.common.exception.auth;

import com.sprint.otboo.common.exception.ErrorCode;

public class InvalidTokenException extends AuthenticationException {

    public InvalidTokenException() {
        super(ErrorCode.INVALID_TOKEN);
    }

    public InvalidTokenException(Throwable cause) {
        super(ErrorCode.INVALID_TOKEN, cause);
    }
}
