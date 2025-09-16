package com.sprint.otboo.auth.jwt;

import java.util.UUID;

public interface TokenProvider {
    String createAccessToken(UUID userId);
}
