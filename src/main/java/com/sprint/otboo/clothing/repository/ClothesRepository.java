package com.sprint.otboo.clothing.repository;

import com.sprint.otboo.clothing.entity.Clothes;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Clothes Repository
 * <p>의상( Clothes ) 엔티티에 대한 기본 CRUD 기능 제공</p>
 */
public interface ClothesRepository extends JpaRepository<Clothes, UUID> {

}
