package com.sprint.otboo.common.exception.auth;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;

public class TokenCreationException extends CustomException {

    public TokenCreationException(ErrorCode errorCode) {
        super(errorCode);
    }
}
