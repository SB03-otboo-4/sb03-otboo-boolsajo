package com.sprint.otboo.clothing.repository;

import com.sprint.otboo.clothing.entity.ClothesAttribute;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesAttributeRepository extends JpaRepository<ClothesAttribute, UUID> {

}
