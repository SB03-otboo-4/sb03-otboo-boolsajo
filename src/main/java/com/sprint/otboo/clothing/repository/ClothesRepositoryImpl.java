package com.sprint.otboo.clothing.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.QClothes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * ClothesRepository Custom 구현체
 *
 * <p>JPAQueryFactory를 활용하여 커서 기반 페이지네이션 및 조건별 의상 조회 제공
 */
@Repository
@RequiredArgsConstructor
public class ClothesRepositoryImpl implements ClothesRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 사용자 의상 조회 (커서 기반 페이지네이션)
     *
     * @param ownerId 조회할 사용자 ID
     * @param type 조회할 의상 타입 (null이면 전체)
     * @param cursor 마지막 조회 시각
     * @param idAfter 마지막 조회 의상 ID
     * @param limit 최대 조회 개수
     * @return 조건에 맞는 의상 리스트
     */
    @Override
    public List<Clothes> findClothesByOwner(UUID ownerId, ClothesType type, Instant cursor, UUID idAfter, int limit
    ) {
        QClothes c = QClothes.clothes;

        // 조회 조건 빌드
        BooleanExpression predicate = buildOwnerPredicate(c, ownerId, type, cursor, idAfter);

        // 최신순 정렬 및 limit 적용
        return queryFactory.selectFrom(c)
            .where(predicate)
            .orderBy(c.createdAt.desc(), c.id.desc())
            .limit(limit)
            .fetch();
    }

    /**
     * 사용자 의상 총 개수 조회
     *
     * @param ownerId 조회할 사용자 ID
     * @param type 조회할 의상 타입 (null이면 전체)
     * @return 의상 총 개수
     */
    @Override
    public long countByOwner(UUID ownerId, ClothesType type) {
        QClothes c = QClothes.clothes;

        BooleanExpression predicate = buildOwnerPredicate(c, ownerId, type, null, null);

        Long count = queryFactory.select(c.count())
            .where(predicate)
            .fetchOne();

        return count != null ? count : 0L;
    }

    /**
     * 조회 조건 빌더
     *
     * <p>커서 기반 페이지네이션과 타입 필터를 통합
     *
     * @param c Q클래스 객체
     * @param ownerId 사용자 ID
     * @param type 의상 타입 필터
     * @param cursor 마지막 조회 시각
     * @param idAfter 마지막 조회 의상 ID
     * @return BooleanExpression 조건식
     */
    private BooleanExpression buildOwnerPredicate(QClothes c, UUID ownerId, ClothesType type,
        Instant cursor, UUID idAfter) {
        BooleanExpression predicate = c.user.id.eq(ownerId);

        // 타입 필터
        if (type != null) {
            predicate = predicate.and(c.type.eq(type));
        }

        // 커서 페이지네이션 조건
        if (cursor != null && idAfter != null) {
            predicate = predicate.and(
                c.createdAt.lt(cursor)
                    .or(c.createdAt.eq(cursor).and(c.id.lt(idAfter)))
            );
        }

        return predicate;
    }
}