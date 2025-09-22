package com.sprint.otboo.common.exception.paging;

import com.sprint.otboo.common.exception.ErrorCode;

public class InvalidPagingParamException extends PagingException {

    public InvalidPagingParamException(ErrorCode errorCode) {
        super(errorCode);
    }
}
