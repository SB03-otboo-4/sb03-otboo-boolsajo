package com.sprint.otboo.clothing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import com.sprint.otboo.clothing.scraper.ClothesExtractionService;
import com.sprint.otboo.clothing.scraper.ClothesExtractor;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClothesExtractionService 단위 테스트")
public class ClothesExtractionServiceTest {

    @Mock
    private ClothesExtractor musinsaExtractor;

    @InjectMocks
    private ClothesExtractionService service;

    @BeforeEach
    void setUp() {
        service = new ClothesExtractionService(List.of(musinsaExtractor));
    }

    @Test
    void 의상_추출_성공_지원하는_URL() {
        // given: 지원하는 URL과 추출 결과 준비
        String url = "https://store.musinsa.com/product/123";
        ClothesDto expectedDto = new ClothesDto(
            UUID.randomUUID(),
            UUID.randomUUID(),
            "테스트 티셔츠",
            "http://image.test.com/1.jpg",
            ClothesType.TOP,
            Collections.emptyList()
        );

        when(musinsaExtractor.supports(url)).thenReturn(true);
        when(musinsaExtractor.extract(url)).thenReturn(expectedDto);

        // when: 서비스 호출
        ClothesDto result = service.extractByUrl(url);

        // then: 기대한 DTO 반환
        assertThat(result).isEqualTo(expectedDto);
    }

    @Test
    void 의상_추출_실패_지원하지_않는_URL() {
        // given: 지원하지 않는 URL
        String url = "https://example.com/product/999";
        when(musinsaExtractor.supports(url)).thenReturn(false);

        // when & then: 예외 발생
        assertThatThrownBy(() -> service.extractByUrl(url))
            .isInstanceOf(ClothesExtractionException.class)
            .hasMessageContaining("지원하지 않는 사이트 URL");
    }
}