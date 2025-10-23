package com.sprint.otboo.dm.service;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.dm.DMException;
import com.sprint.otboo.dm.dto.data.DirectMessageDto;
import com.sprint.otboo.dm.entity.DM;
import com.sprint.otboo.dm.event.DMReceivedEvent;
import com.sprint.otboo.dm.repository.DMRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class DMServiceImpl implements DMService {

    private final DMRepository repository;
    private final ApplicationEventPublisher eventPublisher;

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
        // 입력 검증
        if (Objects.equals(senderId, receiverId)) {
            log.warn("[DM] 자기 자신에게 메시지를 보낼 수 없습니다. (senderId={}, receiverId={})", senderId, receiverId);
            throw new DMException(ErrorCode.SELF_DM_NOT_ALLOWED);
        }
        if (!StringUtils.hasText(content)) {
            log.warn("[DM] 빈 메시지 내용으로 전송 시도됨 (senderId={}, receiverId={}, contentLength={})",
                senderId, receiverId, content == null ? 0 : content.length());
            throw new DMException(ErrorCode.INVALID_INPUT);
        }

        try {
            DM dm = DM.builder()
                .senderId(senderId)
                .receiverId(receiverId)
                .content(content)
                .build();

            DM saved = repository.save(dm);

            eventPublisher.publishEvent(
                new DMReceivedEvent(saved.getId(), senderId, receiverId)
            );

            log.info("[DM] 메시지 전송 성공 (dmId={}, senderId={}, receiverId={}, contentLength={})",
                saved.getId(), senderId, receiverId, content.length());

            return new DirectMessageDto(
                saved.getId(),
                saved.getSenderId(), null, null,
                saved.getReceiverId(), null, null,
                saved.getContent(),
                saved.getCreatedAt() == null ? Instant.now() : saved.getCreatedAt()
            );

        } catch (DMException e) {
            throw e;
        } catch (DataAccessException dae) {
            log.error("[DM] 메시지 저장 중 DB 오류 발생 (senderId={}, receiverId={}, contentLength={}, cause={})",
                senderId, receiverId, content.length(), dae.getClass().getSimpleName(), dae);
            throw new DMException(ErrorCode.INTERNAL_SERVER_ERROR);
        } catch (Exception ex) {
            log.error("[DM] 메시지 전송 중 알 수 없는 오류 발생 (senderId={}, receiverId={}, contentLength={}, cause={})",
                senderId, receiverId, content.length(), ex.getClass().getSimpleName(), ex);
            throw new DMException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}
