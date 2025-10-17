package com.sprint.otboo.feed.repository;

import com.sprint.otboo.feed.entity.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID>, CommentRepositoryCustom {

    List<Comment> findAllByFeedIdOrderByCreatedAtDesc(UUID feedId);
}
