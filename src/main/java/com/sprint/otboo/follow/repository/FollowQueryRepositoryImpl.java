package com.sprint.otboo.follow.repository;

import static com.sprint.otboo.follow.entity.QFollow.follow;

import com.querydsl.core.BooleanBuilder;
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
import org.springframework.util.StringUtils;

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

        BooleanBuilder where = new BooleanBuilder();
        where.and(follow.followerId.eq(followerId));

        if (StringUtils.hasText(nameLike)) {
            where.and(followee.username.containsIgnoreCase(nameLike));
        }

        Instant ts = null;
        if (StringUtils.hasText(cursorCreatedAtIso)) {
            ts = Instant.parse(cursorCreatedAtIso);

            if (idAfter != null) {
                where.and(
                    follow.createdAt.lt(ts)
                        .or(follow.createdAt.eq(ts).and(follow.id.lt(idAfter)))
                );
            } else {
                where.and(follow.createdAt.lt(ts));
            }
        } else {
            // cursor가 없고 idAfter만 있는 경우는 모호하므로 무시 (정렬 규칙상 createdAt tie-break 필요)
        }

        JPAQuery<FollowListItemResponse> query = jpa
            .select(Projections.constructor(FollowListItemResponse.class,
                follow.id,
                Projections.constructor(UserSummaryResponse.class,
                    followee.id, followee.username, followee.profileImageUrl
                ),
                Projections.constructor(UserSummaryResponse.class,
                    followerUser.id, followerUser.username, followerUser.profileImageUrl
                ),
                follow.createdAt
            ))
            .from(follow)
            .join(followee).on(follow.followeeId.eq(followee.id))
            .join(followerUser).on(follow.followerId.eq(followerUser.id))
            .where(where)
            .orderBy(follow.createdAt.desc(), follow.id.desc())
            .limit(limitPlusOne);

        return query.fetch();
    }

    @Override
    public long countFollowing(UUID followerId, String nameLike) {
        QUser followee = new QUser("followeeForCount");

        BooleanBuilder where = new BooleanBuilder();
        where.and(follow.followerId.eq(followerId));
        if (StringUtils.hasText(nameLike)) {
            where.and(followee.username.containsIgnoreCase(nameLike));
        }

        Long cnt = jpa.select(follow.count())
            .from(follow)
            .join(followee).on(follow.followeeId.eq(followee.id))
            .where(where)
            .fetchOne();

        return cnt == null ? 0L : cnt;
    }
}
