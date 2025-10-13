package com.sprint.otboo.notification.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.notification.entity.Notification;
import com.sprint.otboo.notification.entity.QNotification;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<Notification> findByReceiverWithCursor(UUID receiverId, Instant cursor, UUID idAfter, int fetchSize) {
        QNotification notification = QNotification.notification;

        List<Notification> results = queryFactory
            .selectFrom(notification)
            .where(
                notification.receiver.id.eq(receiverId),
                cursorPredicate(notification, cursor, idAfter)
            )
            .orderBy(notification.createdAt.desc(), notification.id.desc())
            .limit(fetchSize)
            .fetch();

        boolean hasNext = results.size() == fetchSize;
        if (hasNext) {
            results = results.subList(0, fetchSize - 1);
        }

        int pageSize = Math.max(fetchSize - 1, 0);
        Pageable pageable = PageRequest.of(
            0,
            pageSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
                .and(Sort.by(Sort.Direction.DESC, "id"))
        );

        return new SliceImpl<>(results, pageable, hasNext);
    }

    private BooleanExpression cursorPredicate(QNotification notification,
        Instant cursorInstant,
        UUID idAfter
    ) {
        if (cursorInstant == null && idAfter == null) {
            return null;
        }

        if (cursorInstant != null && idAfter != null) {
            Instant truncated = cursorInstant.truncatedTo(ChronoUnit.MILLIS);
            // 시간이 cursor보다 이전이거나, 같은 시간대면서 ID가 더 작은 것
            return notification.createdAt.lt(truncated)
                .or(
                    notification.createdAt.goe(truncated)
                        .and(notification.createdAt.lt(truncated.plusMillis(1)))
                        .and(notification.id.lt(idAfter))
                );
        }

        if (cursorInstant != null) {
            Instant truncated = cursorInstant.truncatedTo(ChronoUnit.MILLIS);
            return notification.createdAt.lt(truncated);
        }

        return notification.id.lt(idAfter);
    }
}