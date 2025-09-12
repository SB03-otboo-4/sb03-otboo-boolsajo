package com.sprint.otboo.user.mapper;

import com.sprint.otboo.user.dto.data.ProfileDto;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toUserDto(User user) {
        return new UserDto(
            user.getId(),
            user.getCreatedAt(),
            user.getEmail(),
            user.getUsername(),
            user.getRole(),
            user.getProvider(),
            user.getLocked(),
            user.getProfileImageUrl(),
            user.getProviderId(),
            user.getUpdatedAt()
        );
    }

    public ProfileDto toProfileDto(User user, UserProfile profile) {
        return new ProfileDto(
            user.getId(),
            user.getUsername(),
            user.getProfileImageUrl(),
            profile != null ? profile.getGender() : null,
            profile != null ? profile.getBirthDate() : null,
            profile != null ? profile.getLatitude() : null,
            profile != null ? profile.getLongitude() : null,
            profile != null ? profile.getX() : null,
            profile != null ? profile.getY() : null,
            profile != null ? profile.getLocationNames() : null,
            profile != null ? profile.getTemperatureSensitivity() : null
        );
    }

    public ProfileDto toProfileDto(UserProfile profile) {
        User user = profile.getUser();
        return new ProfileDto(
            profile.getUserId(),
            user.getUsername(),
            user.getProfileImageUrl(),
            profile.getGender(),
            profile.getBirthDate(),
            profile.getLatitude(),
            profile.getLongitude(),
            profile.getX(),
            profile.getY(),
            profile.getLocationNames(),
            profile.getTemperatureSensitivity()
        );
    }
}
