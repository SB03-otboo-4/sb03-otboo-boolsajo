package com.sprint.otboo.common.util;

import java.security.Principal;

/** STOMP 세션에서 name()을 userId(UUID 문자열)로 고정해 전달하기 위한 Principal */
public final class WsPrincipal implements Principal {
    private final String name; // UUID 문자열

    public WsPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }
}
