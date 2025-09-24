package com.sprint.otboo.user.mapper;

import com.sprint.otboo.user.dto.data.ProfileDto;
import com.sprint.otboo.user.dto.data.ProfileLocationDto;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface UserMapper {

    @Mapping(source = "username", target = "name")
    @Mapping(source = "provider", target = "linkedOAuthProviders")
    UserDto toUserDto(User user);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.username", target = "name")
    @Mapping(source = "user.profileImageUrl", target = "profileImageUrl")
    @Mapping(target = "location", expression = "java(toLocation(profile))")
    @Mapping(source = "profile.temperatureSensitivity", target = "temperatureSensitivity")
    ProfileDto toProfileDto(User user, UserProfile profile);

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "user.username", target = "name")
    @Mapping(source = "user.profileImageUrl", target = "profileImageUrl")
    @Mapping(target = "location", expression = "java(toLocation(profile))")
    ProfileDto toProfileDto(UserProfile profile);

    default ProfileLocationDto toLocation(UserProfile profile) {
        if (profile == null) {
            return null;
        }
        boolean hasLocation =
            profile.getLatitude() != null ||
                profile.getLongitude() != null ||
                profile.getX() != null ||
                profile.getY() != null ||
                (profile.getLocationNames() != null && !profile.getLocationNames().isBlank());

        if (!hasLocation) {
            return null;
        }

        return new ProfileLocationDto(
            profile.getLatitude(),
            profile.getLongitude(),
            profile.getX(),
            profile.getY(),
            convertLocationNames(profile.getLocationNames())
        );
    }

    default List<String> convertLocationNames(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.trim().split("\\s+"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }
}