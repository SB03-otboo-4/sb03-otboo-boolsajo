package com.sprint.otboo.clothing.repository;

import com.sprint.otboo.clothing.entity.ClothesAttribute;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ClothesAttribute Repository
 * <p>의상 속성( ClothesAttribute ) 엔티티에 대한 기본 CRUD 기능 제공</p>
 */
public interface ClothesAttributeRepository extends JpaRepository<ClothesAttribute, UUID> {

}
