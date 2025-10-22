package com.sprint.otboo.common.exception.dm;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;

public class DMException extends CustomException {

    public DMException(ErrorCode errorCode) {
        super(errorCode);
    }

    public DMException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
