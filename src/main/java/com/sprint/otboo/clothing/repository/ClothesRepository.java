package com.sprint.otboo.clothing.repository;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Clothes Repository
 * <p>의상( Clothes ) 엔티티에 대한 기본 CRUD 기능 제공</p>
 */
public interface ClothesRepository extends JpaRepository<Clothes, UUID>, ClothesRepositoryCustom {

    List<Clothes> findAllByIdInAndUser_Id(Collection<UUID> ids, UUID userId);

    @Query("select distinct c from Clothes c " +
        "left join fetch c.attributes a " +
        "left join fetch a.definition " +
        "where c.user.id = :userId")
    List<Clothes> findByUserIdWithAttributes(@Param("userId") UUID userId);

    Optional<Clothes> findFirstByType(ClothesType type);

}