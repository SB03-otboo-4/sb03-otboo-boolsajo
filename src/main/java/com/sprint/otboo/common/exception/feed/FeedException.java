package com.sprint.otboo.common.exception.feed;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;

public class FeedException extends CustomException {

    public FeedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FeedException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
