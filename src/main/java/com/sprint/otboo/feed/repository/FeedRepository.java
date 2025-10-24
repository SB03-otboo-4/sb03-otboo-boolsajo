package com.sprint.otboo.feed.repository;

import com.sprint.otboo.feed.entity.Feed;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedRepository extends JpaRepository<Feed, UUID>, FeedRepositoryCustom {

    @Query("""
            select f.id from Feed f
            where f.deleted = true
              and (f.updatedAt > :updatedAt or (f.updatedAt = :updatedAt and f.id > :id))
            order by f.updatedAt asc, f.id asc
        """)
    List<UUID> findDeletedIdsSince(Instant updatedAt, UUID id, Pageable pageable);
}
