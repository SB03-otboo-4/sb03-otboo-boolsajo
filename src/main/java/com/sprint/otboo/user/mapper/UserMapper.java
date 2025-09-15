package com.sprint.otboo.user.mapper;

import com.sprint.otboo.user.dto.data.AuthorDto;
import com.sprint.otboo.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "name", source = "username")
    @Mapping(target = "profileImageUrl", source = "profileImageUrl")
    AuthorDto toAuthorDto(User user);
}
