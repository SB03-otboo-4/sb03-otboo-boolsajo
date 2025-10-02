package com.sprint.otboo.common.exception.follow;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;

public class FollowException extends CustomException {

    public FollowException(ErrorCode errorCode) {
        super(errorCode);
    }
}
