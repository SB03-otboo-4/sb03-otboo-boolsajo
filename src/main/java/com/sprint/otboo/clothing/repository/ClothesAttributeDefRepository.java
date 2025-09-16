package com.sprint.otboo.clothing.repository;

import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesAttributeDefRepository extends JpaRepository<ClothesAttributeDef, UUID> {

}
