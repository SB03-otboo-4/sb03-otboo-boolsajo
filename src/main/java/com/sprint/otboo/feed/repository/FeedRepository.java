package com.sprint.otboo.feed.repository;

import com.sprint.otboo.feed.entity.Feed;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;

@Repository
public interface FeedRepository extends JpaRepository<Feed, UUID> {

}
