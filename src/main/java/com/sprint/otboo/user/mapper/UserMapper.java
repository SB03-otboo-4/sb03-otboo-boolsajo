package com.sprint.otboo.user.mapper;

import com.sprint.otboo.user.dto.data.ProfileDto;
import com.sprint.otboo.user.dto.data.UserDto;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.entity.UserProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

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
    @Mapping(source = "profile.gender", target = "gender")
    @Mapping(source = "profile.birthDate", target = "birthDate")
    @Mapping(source = "profile.latitude", target = "latitude")
    @Mapping(source = "profile.longitude", target = "longitude")
    @Mapping(source = "profile.x", target = "x")
    @Mapping(source = "profile.y", target = "y")
    @Mapping(source = "profile.locationNames", target = "locationNames")
    @Mapping(source = "profile.temperatureSensitivity", target = "temperatureSensitivity")
    ProfileDto toProfileDto(User user, UserProfile profile);

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "user.username", target = "name")
    @Mapping(source = "user.profileImageUrl", target = "profileImageUrl")
    ProfileDto toProfileDto(UserProfile profile);
}