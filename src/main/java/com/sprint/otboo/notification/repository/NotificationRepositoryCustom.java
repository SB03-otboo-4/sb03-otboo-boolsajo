package com.sprint.otboo.notification.repository;

import com.sprint.otboo.notification.entity.Notification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Slice;

public interface NotificationRepositoryCustom {
    /**
     * createdAt/id 조합을 이용한 커서 기반으로 알림을 조회
     *
     * @param receiver 알림 소유자
     * @param cursor createdAt 상한 ( nullable )
     * @param idAfter 동일 시각일 때 비교할 id ( nullable )
     * @param size hasNext 판단용 1개 초과 요소 포함 조회 크기
     * @return Slice 형태 결과
     * */
    Slice<Notification> findByReceiverWithCursor(UUID receiver, Instant cursor, UUID idAfter, int size);
}
