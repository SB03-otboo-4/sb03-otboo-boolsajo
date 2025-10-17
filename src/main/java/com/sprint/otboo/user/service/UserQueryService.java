package com.sprint.otboo.user.service;

import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import java.util.UUID;

public interface UserQueryService {
    UserSummaryResponse getSummary(UUID userId);
}
