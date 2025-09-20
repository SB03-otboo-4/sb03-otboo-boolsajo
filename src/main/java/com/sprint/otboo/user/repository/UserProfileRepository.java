package com.sprint.otboo.user.repository;

import com.sprint.otboo.user.entity.UserProfile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
}
