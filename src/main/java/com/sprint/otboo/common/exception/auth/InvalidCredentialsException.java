package com.sprint.otboo.common.exception.auth;

import com.sprint.otboo.common.exception.ErrorCode;

public class InvalidCredentialsException extends AuthenticationException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
}
