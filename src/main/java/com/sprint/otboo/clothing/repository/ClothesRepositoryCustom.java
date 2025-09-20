package com.sprint.otboo.clothing.repository;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ClothesRepositoryCustom {

    List<Clothes> findClothesByOwner(UUID ownerId, ClothesType type, Instant cursor, UUID idAfter, int limit);

    long countByOwner(UUID ownerId, ClothesType type);

}
