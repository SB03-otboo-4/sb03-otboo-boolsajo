package com.sprint.otboo.auth.jwt;

import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public interface TokenProvider {
    String createAccessToken(UUID userId);
}
