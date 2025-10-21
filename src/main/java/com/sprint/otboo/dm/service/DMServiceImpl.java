package com.sprint.otboo.dm.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.dm.DMException;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.entity.DM;
import com.sprint.otboo.dm.repository.DMRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DMServiceImpl implements DMService {

    private final DMRepository repository;

    @Override
    @Transactional(readOnly = true)
    public CursorPageResponse<DirectMessageDto> getDms(
        UUID me, UUID other, String cursor, UUID idAfter, Integer limitNullable
    ) {
        int limit = (limitNullable == null) ? 20 : Math.max(1, Math.min(100, limitNullable));
        List<DirectMessageDto> list = repository.findDmPageBetween(me, other, cursor, idAfter, limit + 1);

        boolean hasNext = list.size() > limit;
        if (hasNext) list = list.subList(0, limit);

        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && !list.isEmpty()) {
            DirectMessageDto last = list.get(list.size() - 1);
            nextCursor = last.createdAt().toString();
            nextIdAfter = last.id().toString();
        }

        long total = repository.countDmBetween(me, other);
        return new CursorPageResponse<>(
            list, nextCursor, nextIdAfter, hasNext, total, "createdAt", "DESCENDING"
        );
    }

    @Override
    @Transactional
    public DirectMessageDto sendDm(UUID senderId, UUID receiverId, String content) {
        if (Objects.equals(senderId, receiverId)) throw new DMException(ErrorCode.SELF_DM_NOT_ALLOWED);
        if (!StringUtils.hasText(content)) throw new DMException(ErrorCode.INVALID_INPUT);

        DM dm = DM.builder()
            .senderId(senderId)
            .receiverId(receiverId)
            .content(content)
            .build();
        DM saved = repository.save(dm);

        DirectMessageDto dto = new DirectMessageDto(
            saved.getId(),
            saved.getSenderId(), null, null,
            saved.getReceiverId(), null, null,
            saved.getContent(),
            saved.getCreatedAt() == null ? Instant.now() : saved.getCreatedAt()
        );

        return dto;
    }
}
