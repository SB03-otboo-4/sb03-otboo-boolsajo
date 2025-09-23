package com.sprint.otboo.clothing.repository;

import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesAttributeDefRepository extends JpaRepository<ClothesAttributeDef, UUID> {

    Optional<ClothesAttributeDef> findByName(String name);

    List<ClothesAttributeDef> findByNameContainingIgnoreCase(String keyword, Sort sort);
}
