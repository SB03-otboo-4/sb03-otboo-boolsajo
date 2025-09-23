package com.sprint.otboo.clothing.repository;

import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesAttributeDefRepository extends JpaRepository<ClothesAttributeDef, UUID> {

    Optional<ClothesAttributeDef> findByName(String name);

    // 이름 또는 선택값에 keyword 포함 여부 확인
    @Query("SELECT c FROM ClothesAttributeDef c " +
        "WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
        "   OR LOWER(c.selectValues) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<ClothesAttributeDef> findByNameOrSelectValuesContainingIgnoreCase(String keyword, Sort sort);
}
