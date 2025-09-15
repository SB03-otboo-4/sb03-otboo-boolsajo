package com.sprint.otboo.clothing.repository;

import com.sprint.otboo.clothing.entity.Clothes;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothesRepository extends JpaRepository<Clothes, UUID> {

}
