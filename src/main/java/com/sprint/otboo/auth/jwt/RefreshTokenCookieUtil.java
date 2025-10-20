package com.sprint.otboo.auth.jwt;

import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieUtil {

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    public Cookie createRefreshTokenCookie(String refreshToken) {
        Cookie cookie = new Cookie("REFRESH_TOKEN", refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge((int) (refreshTokenExpiration / 1000));
        return cookie;
    }

    public Cookie createExpiredCookie() {
        Cookie cookie = new Cookie("REFRESH_TOKEN", "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        return cookie;
    }

}