package com.sprint.otboo.feed.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.feed.entity.Comment;
import com.sprint.otboo.feed.entity.QComment;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class CommentRepositoryImpl implements CommentRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private static final QComment comment = QComment.comment;

    @Override
    public List<Comment> findByFeedId(UUID feedId, String cursor, UUID idAfter, int limit) {
        log.debug("[CommentRepository] 댓글 조회 시작: feedId={}, cursor={}, idAfter={}, limit={}",
            feedId, cursor,
            idAfter, limit);

        BooleanBuilder where = new BooleanBuilder()
            .and(comment.feed.id.eq(feedId));

        if (cursor != null && idAfter != null) {
            Instant cur;
            try {
                cur = Instant.parse(cursor);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("[CommentRepository] 유효하지 않은 커서: " + cursor, e);
            }
            where.and(
                comment.createdAt.lt(cur)
                    .or(comment.createdAt.eq(cur).and(comment.id.lt(idAfter)))
            );
        } else if (idAfter != null) {
            where.and(comment.id.lt(idAfter));
        }

        List<Comment> result = queryFactory
            .selectFrom(comment)
            .where(where)
            .orderBy(
                comment.createdAt.desc(),
                comment.id.desc()
            )
            .limit(limit + 1L)
            .fetch();

        log.info(
            "[CommentRepository] 댓글 조회 완료: result.size={} (feedId={}, cursor={}, idAfter={}, limit={})",
            result.size(), feedId, cursor, idAfter, limit);

        if (!result.isEmpty()) {
            Comment last = result.get(result.size() - 1);
            log.debug("[CommentRepository] lastComment.id={}, lastComment.createdAt={}",
                last.getId(), last.getCreatedAt());
        }

        return result;
    }

    public long countByFeedId(UUID feedId) {
        Long cnt = queryFactory
            .select(comment.count())
            .from(comment)
            .where(comment.feed.id.eq(feedId))
            .fetchOne();
        return cnt != null ? cnt : 0L;
    }
}
