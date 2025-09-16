package com.sprint.otboo.seeder;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesAttribute;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.user.entity.LoginType;
import com.sprint.otboo.user.entity.Role;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.entity.WindStrength;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeedTestDataSeeder implements DataSeeder {

    private final UserRepository userRepository;
    private final WeatherRepository weatherRepository;
    private final ClothesRepository clothesRepository;
    private final WeatherLocationRepository weatherLocationRepository;
    private final ClothesAttributeDefRepository clothesAttributeDefRepository;

    @Override
    @PostConstruct
    public void seed() {
        // 1) User
        User user = userRepository.findAll().stream().findFirst().orElseGet(() ->
            userRepository.save(User.builder()
                .username("tester")
                .password("password")
                .email("tester@example.com")
                .role(Role.USER)
                .locked(false)
                .profileImageUrl("https://placehold.co/100x100")
                .provider(LoginType.KAKAO)
                .provider(LoginType.KAKAO)
                .build()));

        // 2) WeatherLocation
        WeatherLocation location = weatherLocationRepository.findAll().stream().findFirst()
            .orElseGet(() -> weatherLocationRepository.saveAndFlush(
                WeatherLocation.builder()
                    .latitude(37.5)
                    .longitude(126.9780)
                    .x(60).y(127)
                    .locationNames("서울특별시 중구 태평로1가")
                    .build()));

        // 3) Weather
        Weather weather = weatherRepository.findAll().stream().findFirst().orElseGet(() -> {
            Instant now = Instant.now();
            return weatherRepository.save(Weather.builder()
                .location(location)
                .forecastedAt(now)
                .forecastAt(now)
                .skyStatus(SkyStatus.CLEAR)
                .asWord(WindStrength.WEAK)
                .type(PrecipitationType.NONE)
                .currentC(25.0)
                .probability(0.0)
                .minC(20.0)
                .maxC(28.0)
                .amountMm(0.0)
                .speedMs(1.0)
                .currentPct(0.0)
                .comparedPct(0.0)
                .comparedC(0.0)
                .build());
        });

        // 4) Attribute Definitions (없으면 생성)
        ClothesAttributeDef colorDef = upsertDef("색상", "화이트,블랙,네이비,그레이");
        ClothesAttributeDef sizeDef  = upsertDef("사이즈", "28,30,32,34,FREE");

        // 5) Clothes + Attributes (cascade=ALL이면 별도 repo 필요 없음)
        if (clothesRepository.count() == 0) {
            Clothes shirt = Clothes.builder()
                .user(user)
                .name("화이트 셔츠")
                .imageUrl("https://placehold.co/200x200")
                .type(ClothesType.TOP)
                .build();

            // 편의 메서드가 있으면 shirt.addAttribute(...); 로
            shirt.getAttributes().add(ClothesAttribute.builder()
                .definition(colorDef)
                .value("화이트")
                .clothes(shirt)         // 양방향 세팅
                .build());

            Clothes jean = Clothes.builder()
                .user(user)
                .name("블랙 진")
                .imageUrl("https://placehold.co/200x200")
                .type(ClothesType.BOTTOM)
                .build();

            jean.getAttributes().add(ClothesAttribute.builder()
                .definition(sizeDef)
                .value("32")
                .clothes(jean)
                .build());

            clothesRepository.saveAll(List.of(shirt, jean));
        }

        var clothes = clothesRepository.findAll();
        UUID c1 = clothes.get(0).getId();
        UUID c2 = clothes.size() > 1 ? clothes.get(1).getId() : c1;

        log.info("""
            === FEED TEST SEED ===
            authorId  = {}
            weatherId = {}
            clothesIds= {}, {}
            ======================
            """, user.getId(), weather.getId(), c1, c2);
    }

    private ClothesAttributeDef upsertDef(String name, String selectValuesCsv) {
        Optional<ClothesAttributeDef> found = clothesAttributeDefRepository.findByName(name);
        return found.orElseGet(() ->
            clothesAttributeDefRepository.save(
                ClothesAttributeDef.builder()
                    .name(name)
                    .selectValues(selectValuesCsv) // String (CSV) → 매퍼에서 split(',')
                    .build()
            )
        );
    }
}
