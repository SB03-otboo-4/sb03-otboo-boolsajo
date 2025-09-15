package com.sprint.otboo.clothing.repository;

import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClothesAttributeDefRepository extends JpaRepository<ClothesAttributeDef, UUID> {
    Optional<ClothesAttributeDef> findByName(String name);
}
