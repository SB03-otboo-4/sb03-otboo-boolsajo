package com.sprint.otboo.follow.repository;

import static com.sprint.otboo.follow.entity.QFollow.follow;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import com.sprint.otboo.user.entity.QUser;
import com.sprint.otboo.user.entity.QUserProfile;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FollowQueryRepositoryImpl implements FollowQueryRepository {

    private final JPAQueryFactory jpa;

    @Override
    public List<FollowListItemResponse> findFollowingPage(
        UUID followerId,
        String cursorCreatedAtIso,
        UUID idAfter,
        int limitPlusOne,
        String nameLike
    ) {
        QUser followee = new QUser("followee");
        QUser followerUser = new QUser("followerUser");

        JPAQuery<FollowListItemResponse> query = jpa
            .select(Projections.constructor(FollowListItemResponse.class,
                follow.id,
                Projections.constructor(UserSummaryResponse.class,
                    followee.id,
                    followee.username,
                    followee.profileImageUrl
                ),
                Projections.constructor(UserSummaryResponse.class,
                    followerUser.id,
                    followerUser.username,
                    followerUser.profileImageUrl
                ),
                follow.createdAt
            ))
            .from(follow)
            .join(followee).on(follow.followeeId.eq(followee.id))
            .join(followerUser).on(follow.followerId.eq(followerUser.id))
            .where(follow.followerId.eq(followerId));

        if (nameLike != null && !nameLike.isBlank()) {
            query.where(followee.username.containsIgnoreCase(nameLike));
        }
        if (idAfter != null) {
            query.where(follow.id.lt(idAfter));
        }
        if (cursorCreatedAtIso != null && !cursorCreatedAtIso.isBlank()) {
            Instant ts = Instant.parse(cursorCreatedAtIso);
            // createdAt DESC, id DESC 커서 조건
            query.where(
                follow.createdAt.lt(ts)
                    .or(follow.createdAt.eq(ts).and(follow.id.lt(idAfter)))
            );
        }

        return query
            .orderBy(follow.createdAt.desc(), follow.id.desc())
            .limit(limitPlusOne)
            .fetch();
    }

    @Override
    public long countFollowing(UUID followerId, String nameLike) {
        JPAQuery<Long> query = jpa.select(follow.count())
            .from(follow)
            .where(follow.followerId.eq(followerId));
        if (nameLike != null && !nameLike.isBlank()) {
            QUser followee = new QUser("followeeForCount");
            query.join(followee).on(follow.followeeId.eq(followee.id))
                .where(followee.username.containsIgnoreCase(nameLike));
        }
        Long cnt = query.fetchOne();
        return cnt == null ? 0L : cnt;
    }
}
