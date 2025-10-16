package com.sprint.otboo.notification.listener;

import com.sprint.otboo.clothing.event.ClothesAttributeDefCreatedEvent;
import com.sprint.otboo.clothing.event.ClothesAttributeDefDeletedEvent;
import com.sprint.otboo.feed.event.FeedCommentedEvent;
import com.sprint.otboo.feed.event.FeedCreatedEvent;
import com.sprint.otboo.feed.event.FeedLikedEvent;
import com.sprint.otboo.follow.event.FollowCreatedEvent;
import com.sprint.otboo.notification.service.NotificationService;
import com.sprint.otboo.user.event.UserRoleChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final NotificationService notificationService;

    /**
     * 사용자 권한이 변경되면 해당 사용자에게 권한 변경 알림을 생성
     *
     * @param event 권한이 변경된 사용자 ID와 새로운 Role 정보를 담은 이벤트
     * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRoleChanged(UserRoleChangedEvent event) {
        log.debug("[NotificationListener] handleUserRoleChanged: userId={}, newRole={}",
            event.userId(), event.newRole());

        notificationService.notifyRoleChanged(event.userId(), event.newRole());
    }

    /**
     * 새로운 의류 속성 정의가 추가되면 모든 사용자에게 브로드캐스트 알림을 생성
     *
     * @param event 생성된 속성 이름을 포함한 이벤트
     * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleClothesAttributeCreated(ClothesAttributeDefCreatedEvent event) {
        notificationService.notifyClothesAttributeCreatedForAllUsers(event.attributeName());
    }

    /**
     * 피드에 좋아요가 추가되면 작성자에게 좋아요 사실을 알리는 알림을 생성
     *
     * @param event 피드 작성자와 좋아요 누른 사용자 ID를 담은 이벤트
     * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFeedLiked(FeedLikedEvent event) {
        notificationService.notifyFeedLiked(event.feedAuthorId(), event.likedByUserId());
    }

    /**
     * 피드에 댓글이 달리면 작성자에게 댓글 작성자를 알려주는 알림을 생성
     *
     * @param event 피드 작성자와 댓글 작성자 ID를 담은 이벤트
     * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFeedCommented(FeedCommentedEvent event) {
        notificationService.notifyFeedCommented(event.feedAuthorId(), event.commentedByUserId());
    }

    /**
     * 피드 생성 이벤트를 받아 팔로워 전용 알림 생성을 위임
     *
     * @param event 피드 ID와 작성자 ID가 담긴 이벤트
     * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFeedCreated(FeedCreatedEvent event) {
        log.debug("[NotificationListener] handleFeedCreated: feedId={}, authorId={}",
            event.feedId(), event.authorId());
        notificationService.notifyFollowersFeedCreated(event.authorId(), event.feedId());
    }

    /**
     * 팔로우 생성 이벤트를 받아 새 팔로워 알림 생성을 위임
     *
     * @param event 팔로워 ID와 팔로이 ID가 담긴 이벤트
     * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFollowCreated(FollowCreatedEvent event) {
        log.debug("[NotificationListener] handleFollowCreated: followerId={}, followeeId={}",
            event.followerId(), event.followeeId());
        notificationService.notifyUserFollowed(event.followerId(), event.followeeId());
    }

    /**
     * 의상 속성 정의 삭제 이벤트를 받아 전체 사용자에 대한 알림을 위임
     * */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleClothesAttributeDeleted(ClothesAttributeDefDeletedEvent event) {
        notificationService.notifyClothesAttributeDeletedForAllUsers(event.attributeName());
    }
}
