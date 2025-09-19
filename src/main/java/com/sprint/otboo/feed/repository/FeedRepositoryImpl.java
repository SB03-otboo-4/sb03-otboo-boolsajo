package com.sprint.otboo.feed.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.entity.QFeed;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QFeed feed = QFeed.feed;

    @Override
    public List<Feed> searchByKeyword(
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        String sortDirection,
        String keywordLike,
        SkyStatus skyStatusEqual,
        PrecipitationType precipitationTypeEqual,
        UUID authorIdEqual
    ) {
        boolean desc = !"ASCENDING".equalsIgnoreCase(sortDirection);
        String key = normalizeSortBy(sortBy);

        if ((cursor == null || cursor.isBlank()) && idAfter != null) {
            cursor = null;
        }

        if ("likeCount".equals(key)) {
            return searchByLikeCountCursor(cursor, idAfter, limit, desc,
                keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual);
        } else {
            return searchByCreatedAtCursor(cursor, idAfter, limit, desc,
                keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual);
        }
    }

    private List<Feed> searchByCreatedAtCursor(
        String cursor,
        UUID idAfter,
        int limit,
        boolean desc,
        String keywordLike,
        SkyStatus skyStatusEqual,
        PrecipitationType precipitationTypeEqual,
        UUID authorIdEqual
    ) {
        BooleanBuilder where = buildBaseFilters(keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual);

        BooleanExpression cursorPredicate = buildCreatedAtCursorPredicate(cursor,idAfter, desc);
        if (cursorPredicate != null) {
            where.and(cursorPredicate);
        }
        else if (idAfter != null) {
            where.and(desc ? feed.id.lt(idAfter) : feed.id.gt(idAfter));
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        orders.add(desc ? feed.createdAt.desc() : feed.createdAt.asc());
        orders.add(desc ? feed.id.desc() : feed.id.asc());

        return queryFactory
            .selectFrom(feed)
            .leftJoin(feed.author).fetchJoin()
            .leftJoin(feed.weather).fetchJoin()
            .where(where)
            .orderBy(orders.toArray(new OrderSpecifier[0]))
            .limit(limit + 1L)
            .fetch();
    }

    private List<Feed> searchByLikeCountCursor(
        String cursor,
        UUID idAfter,
        int limit,
        boolean desc,
        String keywordLike,
        SkyStatus skyStatusEqual,
        PrecipitationType precipitationTypeEqual,
        UUID authorIdEqual
    ) {
        BooleanBuilder where = buildBaseFilters(keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual);

        BooleanExpression cursorPredicate = buildLikeCountCursorPredicate(cursor, idAfter, desc);
        if (cursorPredicate != null) {
            where.and(cursorPredicate);
        }
        else if (idAfter != null) {
            where.and(desc ? feed.id.lt(idAfter) : feed.id.gt(idAfter));
        }

        List<OrderSpecifier<?>> orders = new ArrayList<>();
        orders.add(desc ? feed.likeCount.desc() : feed.likeCount.asc());
        orders.add(desc ? feed.id.desc() : feed.id.asc());

        return queryFactory
            .selectFrom(feed)
            .leftJoin(feed.author).fetchJoin()
            .leftJoin(feed.weather).fetchJoin()
            .where(where)
            .orderBy(orders.toArray(new OrderSpecifier[0]))
            .limit(limit + 1L)
            .fetch();
    }

    private static BooleanBuilder buildBaseFilters(
        String keywordLike,
        SkyStatus skyStatusEqual,
        PrecipitationType precipitationTypeEqual,
        UUID authorIdEqual
    ) {
        BooleanBuilder where = new BooleanBuilder();
        if (keywordLike != null && !keywordLike.trim().isEmpty()) {
            where.and(feed.content.containsIgnoreCase(keywordLike.trim()));
        }
        if (skyStatusEqual != null) {
            where.and(feed.weather.skyStatus.eq(skyStatusEqual));
        }
        if (precipitationTypeEqual != null) {
            where.and(feed.weather.type.eq(precipitationTypeEqual));
        }
        if (authorIdEqual != null) {
            where.and(feed.author.id.eq(authorIdEqual));
        }
        return where;
    }

    private static BooleanExpression buildCreatedAtCursorPredicate(String curAt, UUID idAfter, boolean desc) {
        if (curAt == null || idAfter == null) return null;
        Instant cur = Instant.parse(curAt);

        BooleanExpression primary = desc ? feed.createdAt.lt(cur) : feed.createdAt.gt(cur);
        BooleanExpression tie = feed.createdAt.eq(cur)
            .and(desc ? feed.id.lt(idAfter) : feed.id.gt(idAfter));
        return primary.or(tie);
    }

    private static BooleanExpression buildLikeCountCursorPredicate(String curLike, UUID idAfter, boolean desc) {
        if (curLike == null || idAfter == null) return null;
        Long cur = Long.parseLong(curLike);

        BooleanExpression primary = desc ? feed.likeCount.lt(cur) : feed.likeCount.gt(cur);
        BooleanExpression tie = feed.likeCount.eq(cur)
            .and(desc ? feed.id.lt(idAfter) : feed.id.gt(idAfter));
        return primary.or(tie);
    }

    @Override
    public long countByFilters(
        String keywordLike,
        SkyStatus skyStatusEqual,
        PrecipitationType precipitationTypeEqual,
        UUID authorIdEqual
    ) {
        BooleanBuilder where = buildBaseFilters(keywordLike, skyStatusEqual, precipitationTypeEqual, authorIdEqual);

        Long count = queryFactory
            .select(feed.id.count())
            .from(feed)
            .where(where)
            .fetchOne();

        return count != null ? count : 0L;
    }

    private static String normalizeSortBy(String sortBy) {
        if ("likeCount".equalsIgnoreCase(sortBy)) return "likeCount";
        return "createdAt";
    }
}

