package com.sprint.otboo.common.exception.clothing;

import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;

public class ClothesException extends CustomException {

    public ClothesException(ErrorCode errorCode) {
        super(errorCode);
    }
}
