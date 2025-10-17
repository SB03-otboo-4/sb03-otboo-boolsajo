package com.sprint.otboo.notification.listener;

import static org.mockito.BDDMockito.then;

import com.sprint.otboo.clothing.event.ClothesAttributeDefCreatedEvent;
import com.sprint.otboo.clothing.event.ClothesAttributeDefDeletedEvent;
import com.sprint.otboo.feed.event.FeedCommentedEvent;
import com.sprint.otboo.feed.event.FeedCreatedEvent;
import com.sprint.otboo.feed.event.FeedLikedEvent;
import com.sprint.otboo.follow.event.FollowCreatedEvent;
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
    void 의상_속성_추가_이벤트를_받으면_전체_알림을_생성() {
        ClothesAttributeDefCreatedEvent event = new ClothesAttributeDefCreatedEvent("기능성");

        notificationListener.handleClothesAttributeCreated(event);

        then(notificationService).should()
            .notifyClothesAttributeCreatedForAllUsers("기능성");
    }

    @Test
    void 피드_좋아요_이벤트를_받으면_작성자에게_알림을_생성() {
        UUID authorId = UUID.randomUUID();
        UUID likerId = UUID.randomUUID();
        FeedLikedEvent event = new FeedLikedEvent(authorId, likerId);

        notificationListener.handleFeedLiked(event);

        then(notificationService).should().notifyFeedLiked(authorId, likerId);
    }

    @Test
    void 피드_댓글_이벤트를_받으면_작성자에게_알림을_생성() {
        UUID authorId = UUID.randomUUID();
        UUID commenterId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();
        FeedCommentedEvent event = new FeedCommentedEvent(authorId, commenterId, commentId);

        notificationListener.handleFeedCommented(event);

        then(notificationService).should()
            .notifyFeedCommented(authorId, commenterId);
    }

    @Test
    void 피드_생성_이벤트를_받으면_팔로워_알림을_위임() {
        // given
        UUID feedId = UUID.randomUUID();
        UUID authorId = UUID.randomUUID();
        FeedCreatedEvent event = new FeedCreatedEvent(feedId, authorId);

        // when
        notificationListener.handleFeedCreated(event);

        // then
        then(notificationService).should()
            .notifyFollowersFeedCreated(authorId, feedId);
    }

    @Test
    void 팔로우_생성_이벤트를_받으면_새_팔로워_알림을_위임() {
        // given
        UUID followerId = UUID.randomUUID();
        UUID followeeId = UUID.randomUUID();
        FollowCreatedEvent event = new FollowCreatedEvent(followerId, followeeId);

        // when
        notificationListener.handleFollowCreated(event);

        // then
        then(notificationService).should()
            .notifyUserFollowed(followerId, followeeId);
    }

    @Test
    void 의상_속성_삭제_이벤트는_브로드캐스트를_위임() {
        // given
        ClothesAttributeDefDeletedEvent event = new ClothesAttributeDefDeletedEvent("색감");

        // when
        notificationListener.handleClothesAttributeDeleted(event);

        // then
        then(notificationService).should()
            .notifyClothesAttributeDeletedForAllUsers("색감");
    }
}
