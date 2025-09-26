package com.sprint.otboo.notification.listener;

import com.sprint.otboo.clothing.event.ClothesAttributeDefCreatedEvent;
import com.sprint.otboo.feed.event.FeedCommentedEvent;
import com.sprint.otboo.feed.event.FeedLikedEvent;
import com.sprint.otboo.notification.service.NotificationService;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.event.UserRoleChangedEvent;
import com.sprint.otboo.user.repository.UserRepository;
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
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserRoleChanged(UserRoleChangedEvent event) {
        log.debug("[NotificationListener] handleUserRoleChanged: userId={}, newRole={}",
            event.userId(), event.newRole());

        notificationService.notifyRoleChanged(event.userId(), event.newRole());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleClothesAttributeCreated(ClothesAttributeDefCreatedEvent event) {
        notificationService.notifyClothesAttributeCreatedForAllUsers(event.attributeName());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFeedLiked(FeedLikedEvent event) {
        notificationService.notifyFeedLiked(event.feedAuthorId(), event.likedByUserId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFeedCommented(FeedCommentedEvent event) {
        notificationService.notifyFeedCommented(event.feedAuthorId(), event.commentedByUserId());
    }
}
