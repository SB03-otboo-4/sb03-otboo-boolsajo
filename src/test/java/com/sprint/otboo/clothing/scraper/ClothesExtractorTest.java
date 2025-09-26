package com.sprint.otboo.clothing.scraper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.AttributeType;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.util.ClothesAttributeExtractor;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

@DisplayName("ClothesExtractor 단위 테스트")
public class ClothesExtractorTest {

    private MusinsaExtractor musinsaExtractor;
    private ZigzagExtractor zigzagExtractor;

    @Mock
    private ClothesAttributeExtractor attributeExtractor;

    @Mock
    private ClothesAttributeDefRepository defRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        musinsaExtractor = new MusinsaExtractor(attributeExtractor, defRepository);
        zigzagExtractor = new ZigzagExtractor(attributeExtractor, defRepository);
    }

    // ------------------ Musinsa ------------------
    @Test
    void 무신사_URL_지원_여부() {
        // given: 무신사 상품 URL
        String url = "https://www.musinsa.com/product/123";

        // when: supports 호출
        boolean result = musinsaExtractor.supports(url);

        // then: true 반환 확인
        assertThat(result).isTrue();
    }

    @Test
    void Musinsa_의상정보_정상추출() throws IOException {
        // given: 상품 URL과 문서, 속성, DB 정의
        String url = "https://www.musinsa.com/product/123";

        Document doc = mock(Document.class);
        Element titleEl = mock(Element.class);
        when(titleEl.attr("content")).thenReturn("후드 티셔츠");
        when(doc.selectFirst("meta[property=og:title]")).thenReturn(titleEl);

        Element imgEl = mock(Element.class);
        when(imgEl.attr("content")).thenReturn("http://image.jpg");
        when(doc.selectFirst("meta[property=og:image]")).thenReturn(imgEl);

        when(doc.select(".breadcrumb a")).thenReturn(new Elements());

        when(attributeExtractor.extractAttributes(doc, "후드 티셔츠"))
            .thenReturn(
                List.of(new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "RED")));

        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name("COLOR")
            .selectValues("RED,BLUE,BLACK")
            .build();

        when(defRepository.findByName("COLOR")).thenReturn(Optional.of(def));

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = musinsaExtractor.extract(url);

            // then: 의상 정보 정상 추출 확인
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("후드 티셔츠");
            assertThat(result.imageUrl()).isEqualTo("http://image.jpg");
            assertThat(result.type()).isEqualTo(ClothesType.TOP);
            assertThat(result.attributes()).hasSize(1);
            assertThat(result.attributes().get(0).value()).isEqualTo("RED");
        }
    }

    @Test
    void Musinsa_DB정의없는속성_건너뛰기() throws IOException {
        // given: 상품 URL, 문서, 정의 없는 속성
        String url = "https://www.musinsa.com/product/123";

        Document doc = mock(Document.class);
        Element titleEl = mock(Element.class);
        when(titleEl.attr("content")).thenReturn("후드 티셔츠");
        when(doc.selectFirst("meta[property=og:title]")).thenReturn(titleEl);
        when(doc.selectFirst("meta[property=og:image]")).thenReturn(mock(Element.class));
        when(doc.select(".breadcrumb a")).thenReturn(new Elements());

        when(attributeExtractor.extractAttributes(doc, "후드 티셔츠"))
            .thenReturn(
                List.of(new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "UNKNOWN")));
        when(defRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = musinsaExtractor.extract(url);

            // then: 정의 없는 속성 건너뛰기 확인
            assertThat(result.attributes()).isEmpty();
        }
    }

    @Test
    void Musinsa_IOException_발생시_예외변환() throws IOException {
        // given: 상품 URL, Jsoup 연결에서 IOException 발생
        String url = "https://www.musinsa.com/product/123";

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.get()).thenThrow(new IOException("네트워크 오류"));

            // when & then: extract 호출 시 예외 변환 확인
            assertThrows(ClothesExtractionException.class, () -> musinsaExtractor.extract(url));
        }
    }

    // ------------------ Zigzag ------------------

    @Test
    void 지그재그_URL_지원_여부() {
        // given: 지그재그 상품 URL
        String url = "https://www.zigzag.kr/product/123";

        // when: supports 호출
        boolean result = zigzagExtractor.supports(url);

        // then: true 반환 확인
        assertThat(result).isTrue();
    }

    @Test
    void Zigzag_의상정보_정상추출() throws IOException {
        // given: 상품 URL, 문서, 속성, DB 정의
        String url = "https://www.zigzag.kr/product/123";

        Document doc = mock(Document.class);
        Element titleEl = mock(Element.class);
        when(titleEl.attr("content")).thenReturn("에브리띵모던 후드 티셔츠");
        when(doc.selectFirst("meta[property=og:title]")).thenReturn(titleEl);

        Element imgEl = mock(Element.class);
        when(imgEl.attr("content")).thenReturn("http://zigzag-image.jpg");
        when(doc.selectFirst("meta[property=og:image]")).thenReturn(imgEl);

        when(doc.select(".breadcrumb li a")).thenReturn(new Elements());

        when(attributeExtractor.extractAttributes(doc, "에브리띵모던 후드 티셔츠"))
            .thenReturn(
                List.of(new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "RED")));

        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name("COLOR")
            .selectValues("RED,BLUE,BLACK")
            .build();

        when(defRepository.findByName("COLOR")).thenReturn(Optional.of(def));

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = zigzagExtractor.extract(url);

            // then: 의상 정보 정상 추출 확인
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("에브리띵모던 후드 티셔츠");
            assertThat(result.imageUrl()).isEqualTo("http://zigzag-image.jpg");
            assertThat(result.attributes()).hasSize(1);
            assertThat(result.attributes().get(0).value()).isEqualTo("RED");
        }
    }

    @Test
    void Zigzag_DB정의없는속성_건너뛰기() throws IOException {
        // given: 상품 URL, 문서, 정의 없는 속성
        String url = "https://www.zigzag.kr/product/123";

        Document doc = mock(Document.class);
        Element titleEl = mock(Element.class);
        when(titleEl.attr("content")).thenReturn("에브리띵모던 후드 티셔츠");
        when(doc.selectFirst("meta[property=og:title]")).thenReturn(titleEl);
        when(doc.selectFirst("meta[property=og:image]")).thenReturn(mock(Element.class));
        when(doc.select(".breadcrumb li a")).thenReturn(new Elements());

        when(attributeExtractor.extractAttributes(doc, "에브리띵모던 후드 티셔츠"))
            .thenReturn(
                List.of(new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "UNKNOWN")));
        when(defRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = zigzagExtractor.extract(url);

            // then: 정의 없는 속성 건너뛰기 확인
            assertThat(result.attributes()).isEmpty();
        }
    }

    @Test
    void Zigzag_IOException_발생시_예외변환() throws IOException {
        // given: 상품 URL, Jsoup 연결에서 IOException 발생
        String url = "https://www.zigzag.kr/product/123";

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.get()).thenThrow(new IOException("네트워크 오류"));

            // when & then: extract 호출 시 예외 변환 확인
            assertThrows(ClothesExtractionException.class, () -> zigzagExtractor.extract(url));
        }
    }
}