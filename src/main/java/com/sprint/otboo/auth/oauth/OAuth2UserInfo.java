package com.sprint.otboo.auth.oauth;

import com.sprint.otboo.user.entity.LoginType;

public interface OAuth2UserInfo {
    String getProviderUserId();
    LoginType getProvider();
    String getEmail();
    String getName();
}
