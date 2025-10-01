package com.sprint.otboo.auth.oauth;

import com.sprint.otboo.user.entity.LoginType;
import java.util.Map;

public class KakaoUserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;
    private final Map<String, Object> kakaoAccount;
    private final Map<String, Object> properties;

    public KakaoUserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
        this.kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        this.properties = (Map<String, Object>) attributes.get("properties");
    }

    @Override
    public String getProviderUserId() {
        return attributes.get("id").toString();
    }

    @Override
    public LoginType getProvider() {
        return LoginType.KAKAO;
    }

    @Override
    public String getEmail() {
        String email = null;
        if (kakaoAccount != null) {
            email = (String) kakaoAccount.get("email");
        }

        if (email == null) {
            String nickname = getName();
            String providerId = getProviderUserId();
            if (nickname != null) {
                email = nickname.replaceAll("\\s+", "") + "_" + providerId + "@kakao.com";
            }
        }

        return email;
    }

    @Override
    public String getName() {
        if (properties != null) {
            return (String) properties.get("nickname");
        }
        return null;
    }
}
