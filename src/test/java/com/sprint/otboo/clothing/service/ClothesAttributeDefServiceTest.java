package com.sprint.otboo.clothing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sprint.otboo.clothing.dto.request.ClothesAttributeDefCreateRequest;
import com.sprint.otboo.clothing.exception.ClothesValidationException;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@DisplayName("의상 속성 서비스 테스트( ADMIN )")
public class ClothesAttributeDefServiceTest {

    @Autowired
    private ClothesAttributeDefService clothesAttributeDefService;

    @Autowired
    private ClothesAttributeDefRepository repository;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
    }

    @Test
    void 속성_이름이_없으면_등록_실패() {
        // given: 이름이 null인 요청
        ClothesAttributeDefCreateRequest request =
            new ClothesAttributeDefCreateRequest(null, List.of("빨강", "파랑"));

        // when: 요청 실행
        ClothesValidationException thrown = assertThrows(
            ClothesValidationException.class,
            () -> clothesAttributeDefService.createAttributeDef(request)
        );

        // then: 예외 메시지 검증
        assertThat(thrown.getMessage()).isEqualTo("속성 이름은 필수입니다");
    }

    @Test
    void 유효한_요청이면_의상_속성_정의_등록_성공() {
        // given: 유효한 요청
        ClothesAttributeDefCreateRequest request =
            new ClothesAttributeDefCreateRequest("색상", List.of("빨강", "파랑"));

        // when: 요청 실행
        var result = clothesAttributeDefService.createAttributeDef(request);

        // then: 결과 검증
        assertThat(result).isNotNull();
    }
}