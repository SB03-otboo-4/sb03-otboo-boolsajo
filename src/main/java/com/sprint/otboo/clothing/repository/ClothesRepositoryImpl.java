package com.sprint.otboo.clothing.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.QClothes;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ClothesRepositoryImpl implements ClothesRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public ClothesRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<Clothes> findClothesByOwner(UUID ownerId, ClothesType type, Instant cursor, UUID idAfter, int limit
    ) {
        QClothes c =  QClothes.clothes;
        BooleanExpression predicate = c.user.id.eq(ownerId);

        if (type != null) predicate = predicate.and(c.type.eq(type));
        if (cursor != null) {
            predicate = predicate.and(c.createdAt.lt(cursor)
                .or(c.createdAt.eq(cursor).and(c.id.lt(idAfter))));
        }

        return queryFactory.selectFrom(c)
            .where(predicate)
            .orderBy(c.createdAt.desc(), c.id.desc())
            .limit(limit)
            .fetch();
    }

    @Override
    public long countByOwner(UUID ownerId, ClothesType type) {
        QClothes c = QClothes.clothes;
        BooleanExpression predicate = c.user.id.eq(ownerId);
        if (type != null) predicate = predicate.and(c.type.eq(type));

        Long count =  queryFactory
            .select(c.count())
            .where(predicate)
            .fetchOne();

        return count != null ? count : 0L;
    }
}
