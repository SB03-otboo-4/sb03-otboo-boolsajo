package com.sprint.otboo.common.exception.auth;

import com.sprint.otboo.common.exception.ErrorCode;

public class TokenExpiredException extends AuthenticationException {

    public TokenExpiredException() {
        super(ErrorCode.EXPIRED_TOKEN);
    }
}
