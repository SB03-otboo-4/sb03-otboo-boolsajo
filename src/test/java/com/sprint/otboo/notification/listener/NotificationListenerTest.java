package com.sprint.otboo.notification.listener;

import static org.mockito.BDDMockito.then;

import com.sprint.otboo.clothing.event.ClothesAttributeDefCreatedEvent;
import com.sprint.otboo.feed.event.FeedCommentedEvent;
import com.sprint.otboo.feed.event.FeedLikedEvent;
import com.sprint.otboo.notification.service.NotificationService;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.event.UserRoleChangedEvent;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationListener 테스트")
public class NotificationListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationListener notificationListener;

    @Test
    void 권한_변경_이벤트를_받으면_알림_생성을_위임() {
        // given
        UUID userId = UUID.randomUUID();
        UserRoleChangedEvent event = new UserRoleChangedEvent(userId, Role.USER, Role.ADMIN);

        // when
        notificationListener.handleUserRoleChanged(event);

        // then
        then(notificationService).should().notifyRoleChanged(userId, Role.ADMIN);
    }

    @Test
    void 의상_속성_추가_이벤트를_받으면_전체_알림을_생성한다() {
        ClothesAttributeDefCreatedEvent event = new ClothesAttributeDefCreatedEvent("기능성");

        notificationListener.handleClothesAttributeCreated(event);

        then(notificationService).should()
            .notifyClothesAttributeCreatedForAllUsers("기능성");
    }

    @Test
    void 피드_좋아요_이벤트를_받으면_작성자에게_알림을_생성한다() {
        UUID authorId = UUID.randomUUID();
        UUID likerId = UUID.randomUUID();
        FeedLikedEvent event = new FeedLikedEvent(authorId, likerId);

        notificationListener.handleFeedLiked(event);

        then(notificationService).should().notifyFeedLiked(authorId, likerId);
    }

    @Test
    void 피드_댓글_이벤트를_받으면_작성자에게_알림을_생성한다() {
        UUID authorId = UUID.randomUUID();
        UUID commenterId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        FeedCommentedEvent event = new FeedCommentedEvent(authorId, commenterId, commentId);

        notificationListener.handleFeedCommented(event);

        then(notificationService).should()
            .notifyFeedCommented(authorId, commenterId);
    }
}
