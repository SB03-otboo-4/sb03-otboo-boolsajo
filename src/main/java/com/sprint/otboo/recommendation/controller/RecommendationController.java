package com.sprint.otboo.recommendation.controller;

import com.sprint.otboo.auth.jwt.CustomUserDetails;
import com.sprint.otboo.common.exception.CustomException;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.recommendation.dto.data.RecommendationDto;
import com.sprint.otboo.recommendation.service.RecommendationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 의상 추천 관련 API 컨트롤러
 * <p>
 * 사용자의 보유 의상과 날씨 정보를 기반으로 추천 의상을 조회
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * 특정 사용자와 날씨 정보를 기반으로 추천 의상 조회
     *
     * @param weatherId 날씨 정보 ID
     * @return 추천 의상 정보를 담은 DTO
     */
    @GetMapping
    public ResponseEntity<RecommendationDto> getRecommendations(
        @RequestParam UUID weatherId
    ) {
        // SecurityContext에서 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = extractUserId(authentication);

        log.info("추천 요청 수신: 사용자 ID = {}, 날씨 ID = {}", userId, weatherId);
        RecommendationDto dto = recommendationService.getRecommendation(userId, weatherId);

        if (dto == null || dto.clothes().isEmpty()) {
            log.warn("추천 결과 없음: 사용자 ID = {}, 날씨 ID = {}", userId, weatherId);
        } else {
            log.info("추천 결과 존재: 사용자 ID = {}, 추천 의상 개수 = {}", userId, dto.clothes().size());
        }

        return ResponseEntity.ok(dto);
    }

    /**
     * 인증 정보에서 사용자 ID(UUID) 추출
     *
     * @param authentication 인증 객체
     * @return 사용자 UUID
     */
    private UUID extractUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 존재하지 않습니다.");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }

        if (principal instanceof UserDetails userDetails) {
            try {
                return UUID.fromString(userDetails.getUsername());
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.INVALID_INPUT, e);
            }
        }

        if (principal instanceof String principalStr) {
            try {
                return UUID.fromString(principalStr);
            } catch (IllegalArgumentException e) {
                throw new CustomException(ErrorCode.INVALID_INPUT, e);
            }
        }

        throw new CustomException(
            ErrorCode.INVALID_INPUT,
            new IllegalStateException("인증 객체에서 사용자 ID를 추출할 수 없습니다.")
        );
    }
}
