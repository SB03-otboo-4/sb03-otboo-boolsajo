package com.sprint.otboo.auth.oauth;

import com.sprint.otboo.user.entity.LoginType;
import java.util.Map;

public class GoogleUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProviderUserId() {
        return attributes.get("sub").toString();
    }

    @Override
    public LoginType getProvider() {
        return LoginType.GOOGLE;
    }

    @Override
    public String getEmail() {
        return attributes.get("email").toString();
    }

    @Override
    public String getName() {
        return attributes.get("name").toString();
    }
}
