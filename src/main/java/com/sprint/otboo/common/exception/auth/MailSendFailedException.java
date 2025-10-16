package com.sprint.otboo.common.exception.auth;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;

public class MailSendFailedException extends CustomException {

    public MailSendFailedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public MailSendFailedException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
