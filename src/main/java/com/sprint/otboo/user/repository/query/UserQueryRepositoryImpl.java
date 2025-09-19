package com.sprint.otboo.user.repository.query;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.user.entity.QUser;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.service.support.UserListEnums.SortBy;
import com.sprint.otboo.user.service.support.UserListEnums.SortDirection;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    /**
     * 커서 기반 사용자 슬라이스 조회
     * */
    @Override
    public UserSlice findSlice(
        String cursor,
        UUID idAfter,
        int limit,
        SortBy sortBy,
        SortDirection sortDirection,
        String emailLike,
        Role roleEqual,
        Boolean locked
    ) {
        QUser u = QUser.user;

        BooleanBuilder where = new BooleanBuilder();
        if (emailLike != null && !emailLike.isBlank()) {
            where.and(u.email.containsIgnoreCase(emailLike));
        }
        if (roleEqual != null) {
            where.and(u.role.eq(roleEqual));
        }
        if (locked != null) {
            where.and(u.locked.eq(locked));
        }

        // 정렬 구성
        OrderSpecifier<?> primary;
        OrderSpecifier<?> tie;
        if (sortBy == SortBy.EMAIL) {
            primary = sortDirection == SortDirection.DESCENDING ? u.email.desc() : u.email.asc();
            tie     = sortDirection == SortDirection.DESCENDING ? u.id.desc()    : u.id.asc();
        } else { // default CREATED_AT
            primary = sortDirection == SortDirection.DESCENDING ? u.createdAt.desc() : u.createdAt.asc();
            tie     = sortDirection == SortDirection.DESCENDING ? u.id.desc()        : u.id.asc();
        }

        // 경계 조건
        if (cursor != null && !cursor.isBlank()) {
            addCursorPredicate(where, sortBy, sortDirection, cursor, u);
        } else if (idAfter != null) {
            // idAfter로 피벗 하나 가져와 동일한 방식으로 키 구성
            User pivot = queryFactory.selectFrom(u).where(u.id.eq(idAfter)).fetchOne();
            if (pivot != null) {
                String c = (sortBy == SortBy.EMAIL)
                    ? encode("EM", pivot.getEmail(), pivot.getId())
                    : encode("CA", String.valueOf(pivot.getCreatedAt().toEpochMilli()), pivot.getId());
                addCursorPredicate(where, sortBy, sortDirection, c, u);
            }
        }

        // limit+1 전략
        List<User> rows = queryFactory
            .selectFrom(u)
            .where(where)
            .orderBy(primary, tie)
            .limit(limit + 1L)
            .fetch();

        boolean hasNext = rows.size() > limit;
        if (hasNext) rows = rows.subList(0, limit);

        // nextCursor / nextIdAfter 구성
        String nextCursor = null;
        UUID nextIdAfter = null;
        if (!rows.isEmpty()) {
            User last = rows.get(rows.size() - 1);
            nextIdAfter = last.getId();
            nextCursor = (sortBy == SortBy.EMAIL)
                ? encode("EM", last.getEmail(), last.getId())
                : encode("CA", String.valueOf(last.getCreatedAt().toEpochMilli()), last.getId());
        }

        return new UserSlice(rows, hasNext, nextCursor, nextIdAfter);
    }

    /**
     * 목록 전체 개수 조회
     * */
    @Override
    public long countAll(String emailLike, Role roleEqual, Boolean locked) {
        QUser u = QUser.user;
        BooleanBuilder where = new BooleanBuilder();
        if (emailLike != null && !emailLike.isBlank()) where.and(u.email.containsIgnoreCase(emailLike));
        if (roleEqual != null) where.and(u.role.eq(roleEqual));
        if (locked != null) where.and(u.locked.eq(locked));
        Long cnt = queryFactory.select(u.count()).from(u).where(where).fetchOne();
        return cnt == null ? 0L : cnt;
    }

    /**
     * 커서를 해석해서 where 절의 경계조건을 추가
     * */
    private void addCursorPredicate(BooleanBuilder where, SortBy sortBy, SortDirection dir, String cursor, QUser u) {
        Decoded d = decode(cursor);
        if (sortBy == SortBy.EMAIL) {
            String pivotEmail = d.primary(); // primary == email
            if (dir == SortDirection.DESCENDING) {
                where.and(u.email.lt(pivotEmail)
                    .or(u.email.eq(pivotEmail).and(u.id.lt(d.id()))));
            } else {
                where.and(u.email.gt(pivotEmail)
                    .or(u.email.eq(pivotEmail).and(u.id.gt(d.id()))));
            }
        } else { // CREATED_AT
            long millis = Long.parseLong(d.primary()); // primary == createdAt millis
            Instant pivot = Instant.ofEpochMilli(millis);
            if (dir == SortDirection.DESCENDING) {
                where.and(u.createdAt.lt(pivot)
                    .or(u.createdAt.eq(pivot).and(u.id.lt(d.id()))));
            } else {
                where.and(u.createdAt.gt(pivot)
                    .or(u.createdAt.eq(pivot).and(u.id.gt(d.id()))));
            }
        }
    }

    private String encode(String tag, String primary, UUID id) {
        String raw = tag + "|" + primary + "|" + id;
        return Base64.getUrlEncoder().withoutPadding()
            .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private Decoded decode(String cursor) {
        String raw = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
        String[] p = raw.split("\\|", 3);
        // p[0] = "CA" or "EM"
        return new Decoded(p[0], p[1], UUID.fromString(p[2]));
    }

    private record Decoded(String tag, String primary, UUID id) {}
}
