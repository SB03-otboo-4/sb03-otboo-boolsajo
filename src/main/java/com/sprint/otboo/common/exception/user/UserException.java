package com.sprint.otboo.common.exception.user;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;

public class UserException extends CustomException {

    public UserException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UserException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
