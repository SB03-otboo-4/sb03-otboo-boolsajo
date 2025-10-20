package com.sprint.otboo.dm.repository;

import com.sprint.otboo.dm.entity.DM;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DMRepository extends JpaRepository<DM, UUID>, DMQueryRepository {

}