package com.sprint.otboo.common.exception.paging;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;

public class PagingException extends CustomException {

    public PagingException(ErrorCode errorCode) {
        super(errorCode);
    }

    public PagingException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}