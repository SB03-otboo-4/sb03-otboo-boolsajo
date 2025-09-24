package com.sprint.otboo.recommendation.service;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.entity.ClothesType;
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
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 사용자의 의상 목록과 날씨 정보를 기반으로
 * 추천 의상을 생성하는 서비스 구현체
 *
 * <ul>
 *   <li>날씨 정보 조회</li>
 *   <li>사용자 의상 조회</li>
 *   <li>사용자 프로필 기반 체감온도 계산</li>
 *   <li>추천 엔진을 통해 추천 의상 추출</li>
 * </ul>
 */
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
     * 사용자 ID와 날씨 ID를 기반으로 추천 의상을 생성하고 반환
     *
     * @param userId 사용자 ID
     * @param weatherId 날씨 ID
     * @return 추천 의상 정보가 담긴 RecommendationDto
     * @throws CustomException 존재하지 않는 사용자/날씨일 경우
     */
    @Override
    public RecommendationDto getRecommendation(UUID userId, UUID weatherId) {
        // 1. 날씨 조회
        Weather weather = weatherRepository.findById(weatherId)
            .orElseThrow(() -> new CustomException(ErrorCode.WEATHER_NOT_FOUND));

        // 2. 사용자 의상 조회
        List<Clothes> userClothes = clothesRepository.findByUser_Id(userId);

        // 3. 사용자 프로필 조회 (온도 민감도)
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 4. 체감 온도 계산
        //    - maxTemp/minTemp: 최고/최저 기온
        //    - windSpeed: 풍속
        //    - windFactor: 바람 영향 보정계수 (기본 0.8)
        //    - sensitivity: 사용자 온도 민감도
        double perceivedTemp = WeatherUtils.calculatePerceivedTemperature(
            weather.getMaxC(), weather.getMinC(),
            weather.getSpeedMs(), 0.8,
            profile.getTemperatureSensitivity()
        );

        // 5. 이전 추천 조회 → 재추천 여부 판단
        boolean excludeDress = recommendationRepository
            .findTopByUser_IdOrderByCreatedAtDesc(userId)
            .map(prev -> prev.getRecommendationClothes().stream()
                .anyMatch(rc -> rc.getClothes().getType() == ClothesType.DRESS))
            .orElse(false);

        // 6. 추천 엔진 실행 → 사용자의 옷 중 체감 온도에 적합한 옷 필터링
        List<Clothes> recommended = recommendationEngine.recommend(userClothes, perceivedTemp, weather, excludeDress);

        // 7. 추천 엔티티 구성
        Recommendation recommendation = Recommendation.builder()
            .user(User.builder().id(userId).build())
            .weather(weather)
            .build();

        // 8. 추천된 옷들을 RecommendationClothes로 묶어서 연결
        recommended.forEach(c -> recommendation.addRecommendationClothes(
            RecommendationClothes.builder()
                .clothes(c)
                .recommendation(recommendation)
                .build()
        ));

        // 9. 엔티티 → DTO 변환 후 반환
        return recommendationMapper.toDto(recommendation);
    }
}