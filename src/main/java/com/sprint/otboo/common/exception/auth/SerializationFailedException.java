package com.sprint.otboo.common.exception.auth;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;

public class SerializationFailedException extends CustomException {

    public SerializationFailedException(Throwable cause) {
        super(ErrorCode.SERIALIZATION_FAILED, cause);
    }
}
