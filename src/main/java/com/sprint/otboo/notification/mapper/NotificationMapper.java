package com.sprint.otboo.notification.mapper;

import com.sprint.otboo.notification.dto.response.NotificationDto;
import com.sprint.otboo.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(source = "receiver.id", target = "receiverId")
    NotificationDto toDto(Notification notification);
}
