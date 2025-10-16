package com.sprint.otboo.follow.repository;

import static com.sprint.otboo.follow.entity.QFollow.follow;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.follow.dto.response.FollowListItemResponse;
import com.sprint.otboo.user.dto.response.UserSummaryResponse;
import com.sprint.otboo.user.entity.QUser;
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

    /** createdAt DESC, id DESC 기준 커서 조건을 where에 추가 */
    private void applyCursor(BooleanBuilder where, String cursorCreatedAtIso, UUID idAfter) {
        if (!StringUtils.hasText(cursorCreatedAtIso)) return;
        Instant ts = Instant.parse(cursorCreatedAtIso);
        if (idAfter != null) {
            where.and(
                follow.createdAt.lt(ts)
                    .or(follow.createdAt.eq(ts).and(follow.id.lt(idAfter)))
            );
        } else {
            where.and(follow.createdAt.lt(ts));
        }
    }

    @Override
    public List<FollowListItemResponse> findFollowingPage(
        UUID followerId, String cursorCreatedAtIso, UUID idAfter, int limitPlusOne, String nameLike
    ) {
        QUser followee = new QUser("followee");
        QUser followerUser = new QUser("followerUser");

        BooleanBuilder where = new BooleanBuilder().and(follow.followerId.eq(followerId));
        if (StringUtils.hasText(nameLike)) {
            where.and(followee.username.containsIgnoreCase(nameLike));
        }
        applyCursor(where, cursorCreatedAtIso, idAfter);

        return jpa
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
            .limit(limitPlusOne)
            .fetch();
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

    @Override
    public List<FollowListItemResponse> findFollowersPage(
        UUID followeeId, String cursorCreatedAtIso, UUID idAfter, int limitPlusOne, String nameLike
    ) {
        QUser followerUser = new QUser("followerUser");

        BooleanBuilder where = new BooleanBuilder()
            .and(follow.followeeId.eq(followeeId));

        if (StringUtils.hasText(nameLike)) {
            where.and(followerUser.username.containsIgnoreCase(nameLike));
        }

        if (StringUtils.hasText(cursorCreatedAtIso)) {
            Instant ts = Instant.parse(cursorCreatedAtIso);
            if (idAfter != null) {
                where.and(
                    follow.createdAt.lt(ts)
                        .or(follow.createdAt.eq(ts).and(follow.id.lt(idAfter)))
                );
            } else {
                where.and(follow.createdAt.lt(ts));
            }
        }

        Class<?>[] ctorParamTypes = new Class<?>[] {
            UUID.class,                // follow id
            UserSummaryResponse.class, // followeeSummary (null 참조)
            UserSummaryResponse.class, // followerSummary
            Instant.class              // createdAt
        };

        return jpa
            .select(Projections.constructor(
                FollowListItemResponse.class,
                ctorParamTypes,
                follow.id,
                Expressions.nullExpression(UserSummaryResponse.class),
                Projections.constructor(UserSummaryResponse.class,
                    followerUser.id, followerUser.username, followerUser.profileImageUrl
                ),
                follow.createdAt
            ))
            .from(follow)
            .join(followerUser).on(follow.followerId.eq(followerUser.id))
            .where(where)
            .orderBy(follow.createdAt.desc(), follow.id.desc())
            .limit(limitPlusOne)
            .fetch();
    }

    @Override
    public long countFollowers(UUID followeeId, String nameLike) {
        QUser followerUser = new QUser("followerUser");

        BooleanBuilder where = new BooleanBuilder();
        where.and(follow.followeeId.eq(followeeId));

        if (StringUtils.hasText(nameLike)) {
            where.and(followerUser.username.containsIgnoreCase(nameLike));
        }

        Long cnt = jpa.select(follow.count())
            .from(follow)
            .join(followerUser).on(follow.followerId.eq(followerUser.id))
            .where(where)
            .fetchOne();

        return cnt == null ? 0L : cnt;
    }
}
