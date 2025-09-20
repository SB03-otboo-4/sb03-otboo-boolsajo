package com.sprint.otboo.common.exception.clothing;

import com.sprint.otboo.common.exception.ErrorCode;
import java.util.List;
import java.util.UUID;

public class UserClothesNotFoundException extends ClothesException {

    public UserClothesNotFoundException() {
        super(ErrorCode.USER_CLOTHES_NOT_FOUND);
    }

    public static UserClothesNotFoundException withId(UUID userId) {
        UserClothesNotFoundException exception = new UserClothesNotFoundException();
        exception.addDetail("userId", userId);
        return exception;
    }

    public static UserClothesNotFoundException withIds(UUID userId, List<UUID> missingClothesIds) {
        UserClothesNotFoundException exception = new UserClothesNotFoundException();
        exception.addDetail("userId", userId);
        exception.addDetail("missingClothesIds", missingClothesIds);
        return exception;
    }
}
