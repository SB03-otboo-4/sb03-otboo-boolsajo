package com.sprint.otboo.feed.mapper;

import com.sprint.otboo.feed.dto.data.CommentDto;
import com.sprint.otboo.feed.entity.Comment;
import com.sprint.otboo.user.mapper.AuthorMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(
    componentModel = "spring",
    uses = { AuthorMapper.class }
)
public interface CommentMapper {

    @Mapping(target = "feedId", source = "feed.id")
    @Mapping(target = "author", source = "author")
    CommentDto toDto(Comment comment);
}
