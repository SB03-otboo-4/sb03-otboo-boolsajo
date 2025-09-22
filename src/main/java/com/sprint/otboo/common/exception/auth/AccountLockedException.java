package com.sprint.otboo.common.exception.auth;

import com.sprint.otboo.common.exception.ErrorCode;
import java.util.UUID;

public class AccountLockedException extends AuthenticationException {
    public AccountLockedException() {
        super(ErrorCode.ACCOUNT_LOCKED);
    }

    public static AccountLockedException withId(UUID userId) {
        AccountLockedException e = new AccountLockedException();
        e.addDetail("userId", userId);
        return e;
    }
}
