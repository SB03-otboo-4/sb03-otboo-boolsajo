package com.sprint.otboo.common.exception.weather;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;

public class WeatherException extends CustomException {

    protected WeatherException(ErrorCode errorCode) {
        super(errorCode);
    }

    protected WeatherException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    @Override
    public String getMessage() {
        return getErrorCode().name() + " " + super.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return getMessage();
    }
}

