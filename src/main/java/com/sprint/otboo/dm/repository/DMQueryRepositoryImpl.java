package com.sprint.otboo.dm.repository;

import static com.sprint.otboo.dm.entity.QDM.dM;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.user.entity.QUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

@Repository
@RequiredArgsConstructor
public class DMQueryRepositoryImpl implements DMQueryRepository {

    private final JPAQueryFactory jpa;

    @Override
    public List<DirectMessageDto> findDmPageBetween(
        UUID me, UUID other, String cursorCreatedAtIso, UUID idAfter, int limitPlusOne
    ) {
        QUser sender = new QUser("sender");
        QUser receiver = new QUser("receiver");

        BooleanBuilder where = new BooleanBuilder()
            .and(
                dM.senderId.eq(me).and(dM.receiverId.eq(other))
                    .or(dM.senderId.eq(other).and(dM.receiverId.eq(me)))
            );

        if (StringUtils.hasText(cursorCreatedAtIso)) {
            Instant ci = Instant.parse(cursorCreatedAtIso);
            where.and(dM.createdAt.lt(ci)
                .or(dM.createdAt.eq(ci).and(dM.id.lt(idAfter))));
        }

        return jpa.select(Projections.constructor(DirectMessageDto.class,
                dM.id,
                dM.senderId,
                sender.username,
                sender.profileImageUrl,
                dM.receiverId,
                receiver.username,
                receiver.profileImageUrl,
                dM.content,
                dM.createdAt
            ))
            .from(dM)
            .join(sender).on(dM.senderId.eq(sender.id))
            .join(receiver).on(dM.receiverId.eq(receiver.id))
            .where(where)
            .orderBy(dM.createdAt.desc(), dM.id.desc())
            .limit(limitPlusOne)
            .fetch();
    }

    @Override
    public long countDmBetween(UUID me, UUID other) {
        BooleanBuilder where = new BooleanBuilder()
            .and(
                dM.senderId.eq(me).and(dM.receiverId.eq(other))
                    .or(dM.senderId.eq(other).and(dM.receiverId.eq(me)))
            );
        return jpa.select(dM.count()).from(dM).where(where).fetchFirst();
    }
}
