package com.sprint.otboo.recommendation.service;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.Season;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.entity.Recommendation;
import com.sprint.otboo.recommendation.entity.RecommendationClothes;
import com.sprint.otboo.recommendation.mapper.RecommendationMapper;
import com.sprint.otboo.recommendation.repository.RecommendationRepository;
import com.sprint.otboo.recommendation.util.RecommendationEngine;
import com.sprint.otboo.recommendation.util.WeatherUtils;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.entity.UserProfile;
import com.sprint.otboo.user.repository.UserProfileRepository;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 의상 목록과 날씨 정보를 기반으로 추천 의상을 생성하는 서비스 구현체
 *
 * <p>주요 기능:
 * <ul>
 *   <li>날씨 정보 조회</li>
 *   <li>사용자 의상 조회</li>
 *   <li>사용자 프로필 기반 체감온도 계산</li>
 *   <li>최근 추천 기록을 반영하여 추천 엔진 실행</li>
 *   <li>누락된 의상 타입에 대해 확률적 Fallback 적용</li>
 *   <li>Dress ↔ Top & Bottom 상호 배타 규칙 유동적 적용</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final RecommendationMapper recommendationMapper;
    private final ClothesRepository clothesRepository;
    private final WeatherRepository weatherRepository;
    private final UserProfileRepository userProfileRepository;
    private final RecommendationEngine recommendationEngine;

    /**
     * 사용자 ID와 날씨 ID를 기반으로 추천 의상을 생성하고 DTO로 반환
     *
     * @param userId 추천 대상 사용자 ID
     * @param weatherId 참조할 날씨 ID
     * @return 추천된 의상 정보가 담긴 {@link RecommendationDto}
     * @throws CustomException 사용자 또는 날씨 정보가 존재하지 않을 경우
     */
    @Override
    @Transactional
    public RecommendationDto getRecommendation(UUID userId, UUID weatherId) {
        // 1. 날씨 조회
        Weather weather = weatherRepository.findByIdWithLocation(weatherId)
            .orElseThrow(() -> new CustomException(ErrorCode.WEATHER_NOT_FOUND));

        // 2. 사용자 의상 조회
        List<Clothes> userClothes = clothesRepository.findByUserIdWithAttributes(userId);

        // 2-1. 사용자의 옷장이 비어있는 경우
        if (userClothes.isEmpty()) {
            log.warn("[Recommendation] 사용자 의상 없음 → 추천 진행 불가, 빈 DTO 반환: 사용자 ID = {}", userId);
            Recommendation emptyRecommendation = Recommendation.builder()
                .user(User.builder().id(userId).build())
                .weather(weather)
                .build();
            return recommendationMapper.toDto(emptyRecommendation);
        }

        // 3. 사용자 프로필 조회 (온도 민감도)
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 4. 체감 온도 계산
        //    - 최고ㆍ최저 온도 존재 시: 해당 값 기준으로 체감 온도 계산
        //    - 최고ㆍ최저 온도 없을 시: 현재 온도 기준으로 체감 온도 계산
        //    - 풍속 및 사용자 온도 민감도 반영
        //    - 계산 결과 로그 출력 및 체감 온도 기준 계절 판별
        double perceivedTemp;
        if (weather.getMaxC() != null && weather.getMinC() != null) {
            // 4-1. 최고ㆍ최저 온도 존재 시
            perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
                weather.getMaxC(),
                weather.getMinC(),
                weather.getSpeedMs() != null ? weather.getSpeedMs() : 0.0,
                0.8,
                profile.getTemperatureSensitivity()
            );

            // 4-2. 체감 온도 로그 (최고ㆍ최저 온도 기준)
            log.info("[Recommendation] 체감 온도: {}°C, 최고기온: {}°C, 최저기온: {}°C, 풍속: {}m/s, 구름 상태: {}, 풍속 강도: {}, 강수 유형: {}",
                perceivedTemp,
                weather.getMaxC(),
                weather.getMinC(),
                weather.getSpeedMs() != null ? weather.getSpeedMs() : 0.0,
                weather.getSkyStatus(),
                weather.getAsWord(),
                weather.getType()
            );
        } else {
            // 4-1. 최고ㆍ최저 온도 없을 시: 현재 온도 사용
            double currentTemp = weather.getCurrentC() != null ? weather.getCurrentC() : 0.0;
            perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
                currentTemp,
                weather.getSpeedMs() != null ? weather.getSpeedMs() : 0.0,
                0.8,
                profile.getTemperatureSensitivity()
            );

            // 4-2. 체감 온도 로그 (현재 온도 기준)
            log.info("[Recommendation] 체감 온도(현재 온도로 계산): {}°C, 현재온도: {}°C, 풍속: {}m/s, 구름 상태: {}, 풍속 강도: {}, 강수 유형: {}",
                perceivedTemp,
                currentTemp,
                weather.getSpeedMs() != null ? weather.getSpeedMs() : 0.0,
                weather.getSkyStatus(),
                weather.getAsWord(),
                weather.getType()
            );
        }

        // 4-3. 체감 온도 기반 계절 판별 로그
        Season season = WeatherUtils.classifySeason(perceivedTemp);
        log.info("[Recommendation] 체감 온도 기준 판별된 계절: {}", season);

        // 5. 최근 10분 내 추천 이력 조회
        Instant cutoff = LocalDateTime.now()
            .minusMinutes(10)
            .atZone(ZoneId.systemDefault())
            .toInstant();
        List<Recommendation> recentRecommendations =
            recommendationRepository.findByUser_IdAndCreatedAtAfter(userId, cutoff);

        // 6. 최근 추천된 옷 ID 수집
        Set<UUID> recentlyRecommendedIds = recentRecommendations.stream()
            .flatMap(r -> r.getRecommendationClothes().stream())
            .map(rc -> rc.getClothes().getId())
            .collect(Collectors.toSet());

        // 7. 최근 추천된 옷 제외
        List<Clothes> filteredClothes = userClothes.stream()
            .filter(c -> !recentlyRecommendedIds.contains(c.getId()))
            .toList();

        // 7-1. 최소 1개 추천 보장 (랜덤 선택으로 다양성 유지)
        if (filteredClothes.isEmpty() && !userClothes.isEmpty()) {
            // 사용자 의상 중 하나를 랜덤으로 선택
            Random random = new Random();
            Clothes fallbackClothes = userClothes.get(random.nextInt(userClothes.size()));
            filteredClothes = List.of(fallbackClothes);
            log.info("[Recommendation] 최소 1개 추천 보장: {}", fallbackClothes.getName());
        }

        // 8. 최근 추천 제외 후 비어있으면 전체로 재시도 (추천할 옷 부족 시)
        if (filteredClothes.isEmpty()) {
            log.info("[Recommendation] 최근 추천 제외 후 남은 의상이 없어 재추천 허용으로 전환");
            filteredClothes = userClothes;
        }

        // 9. 추천 엔진 실행 → 사용자의 옷 중 체감 온도에 적합한 옷 필터링
        List<Clothes> recommended = new ArrayList<>(recommendationEngine.recommend(
            filteredClothes, perceivedTemp, weather, false
        ));

        // 10. 추천된 의상 타입 로그
        if (!recommended.isEmpty()) {
            String types = recommended.stream()
                .map(Clothes::getType)
                .map(Enum::name)
                .distinct()
                .collect(Collectors.joining(", "));
            log.info("[Recommendation] 추천된 의상 타입: {}", types);
        }

        // 10-1. Fallback 적용: 누락된 타입 보충
        Random random = new Random();
        int fallbackCount = 0;

        for (ClothesType type : ClothesType.values()) {
            boolean hasType = recommended.stream().anyMatch(c -> c.getType() == type);
            if (!hasType) {
                // 사용자 소유 의상 중 해당 타입 검색
                List<Clothes> fallbackCandidates = userClothes.stream()
                    .filter(c -> c.getType() == type)
                    .toList();

                if (!fallbackCandidates.isEmpty() && random.nextDouble() < 0.5) {
                    // 하나 랜덤 선택
                    Clothes fallback = fallbackCandidates.get(random.nextInt(fallbackCandidates.size()));
                    recommended.add(fallback);
                    log.info("[Fallback] {} 타입 의상 미존재 → '{}' 대체 추천 (확률적 적용)", type, fallback.getName());
                    fallbackCount++;
                }
            }
        }
        // 10-2. Fallback 적용 요약 로그
        log.info("[Fallback Summary] {}개 타입 의상 Fallback 적용", fallbackCount);

        // 10-3. 상호 배타 적용
        applyMutualExclusion(recommended, recentRecommendations);

        // 11. 추천 엔티티 구성
        Recommendation recommendation = Recommendation.builder()
            .user(User.builder().id(userId).build())
            .weather(weather)
            .build();

        // 12. 추천된 옷들을 RecommendationClothes로 묶어서 연결
        recommended.forEach(c -> recommendation.addRecommendationClothes(
            RecommendationClothes.builder()
                .clothes(c)
                .recommendation(recommendation)
                .build()
        ));

        // 13. 추천 이력 저장
        recommendationRepository.save(recommendation);

        // 14. 엔티티 → DTO 변환 후 반환
        return recommendationMapper.toDto(recommendation);
    }

    /**
     * Dress와 Top&Bottom 간의 상호 배타 규칙을 최근 추천 기록 기준으로 유동적으로 적용
     *
     * <p>규칙:
     * <ul>
     *   <li>최근 n회 추천 내역을 기반으로 Dress 또는 Top & Bottom 과다 출현 시 이번 추천에서 일부 제거</li>
     *   <li>추천 다양성을 위해 매번 무조건 제거하지 않고 조건부 적용</li>
     * </ul>
     *
     * @param recommended 이번 추천 목록
     * @param recentRecommendations 최근 추천 기록
     */
    private void applyMutualExclusion(List<Clothes> recommended, List<Recommendation> recentRecommendations) {
        // 최근 3회 기준
        int recentLimit = 3;

        // 최근 추천 기록을 생성일 기준 내림차순으로 정렬하고, 최근 3회만 추출
        List<Recommendation> recent = recentRecommendations.stream()
            .sorted(Comparator.comparing(Recommendation::getCreatedAt).reversed())
            .limit(recentLimit)
            .toList();

        // 최근 N회 추천 내에서 Dress 타입 의상 등장 횟수 계산
        long dressCount = recent.stream()
            .flatMap(r -> r.getRecommendationClothes().stream())
            .filter(rc -> rc.getClothes().getType() == ClothesType.DRESS)
            .count();

        // 최근 N회 추천 내에서 Top 또는 Bottom 타입 의상 등장 횟수 계산
        long topOrBottomCount = recent.stream()
            .flatMap(r -> r.getRecommendationClothes().stream())
            .filter(rc -> rc.getClothes().getType() == ClothesType.TOP
                || rc.getClothes().getType() == ClothesType.BOTTOM)
            .count();

        // Dress가 최근 N회 중 2회 이상 추천되었고, 이번 추천에 Top&Bottom 포함 시 Dress 제거
        if (dressCount >= 2 && recommended.stream().anyMatch(c -> c.getType() == ClothesType.TOP || c.getType() == ClothesType.BOTTOM)) {
            recommended.removeIf(c -> c.getType() == ClothesType.DRESS);
            log.info("[MutualExclusion] Dress 과다 추천 → 이번 추천에서 Dress 제거");
        }
        // Top&Bottom이 최근 N회 중 2회 이상 추천되었고, 이번 추천에 Dress 포함 시 Top&Bottom 제거
        else if (topOrBottomCount >= 2 && recommended.stream().anyMatch(c -> c.getType() == ClothesType.DRESS)) {
            recommended.removeIf(c -> c.getType() == ClothesType.TOP || c.getType() == ClothesType.BOTTOM);
            log.info("[MutualExclusion] Top&Bottom 과다 추천 → 이번 추천에서 Top&Bottom 제거");
        }
    }
}