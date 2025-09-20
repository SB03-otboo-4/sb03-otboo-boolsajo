package com.sprint.otboo.seeder;

import com.sprint.otboo.clothing.entity.*;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("dev")
@Order(3)
public class ClothesTestDataSeeder implements DataSeeder {

    private final ClothesRepository clothesRepository;
    private final ClothesAttributeDefRepository clothesAttributeDefRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void seed() {
        if (clothesRepository.count() > 0) {
            log.info("[ClothesSeeder] skip (already has {} items)", clothesRepository.count());
            return;
        }

        User user = userRepository.findAll().stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Seed user not found. Run UserTestDataSeeder first."));

        ClothesAttributeDef colorDef = upsertDef("색상", "화이트,블랙,네이비,그레이");
        ClothesAttributeDef sizeDef  = upsertDef("사이즈", "28,30,32,34,FREE");

        Clothes shirt = Clothes.builder()
            .user(user)
            .name("화이트 셔츠")
            .imageUrl("https://placehold.co/200x200")
            .type(ClothesType.TOP)
            .build();
        shirt.getAttributes().add(ClothesAttribute.builder()
            .definition(colorDef).value("화이트").clothes(shirt).build());

        Clothes jean = Clothes.builder()
            .user(user)
            .name("블랙 진")
            .imageUrl("https://placehold.co/200x200")
            .type(ClothesType.BOTTOM)
            .build();
        jean.getAttributes().add(ClothesAttribute.builder()
            .definition(sizeDef).value("32").clothes(jean).build());

        clothesRepository.saveAll(List.of(shirt, jean));

        var clothes = clothesRepository.findAll();
        UUID c1 = clothes.get(0).getId();
        UUID c2 = clothes.size() > 1 ? clothes.get(1).getId() : c1;

        log.info("""
            [ClothesSeeder]
            authorId  = {}
            clothesIds= {}, {}
            """, user.getId(), c1, c2);
    }

    private ClothesAttributeDef upsertDef(String name, String selectValuesCsv) {
        Optional<ClothesAttributeDef> found = clothesAttributeDefRepository.findByName(name);
        return found.orElseGet(() ->
            clothesAttributeDefRepository.save(
                ClothesAttributeDef.builder()
                    .name(name)
                    .selectValues(selectValuesCsv)
                    .build()
            )
        );
    }
}
