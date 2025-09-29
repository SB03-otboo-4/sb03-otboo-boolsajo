package com.sprint.otboo.clothing.scraper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.sprint.otboo.clothing.dto.data.ClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ClothesDto;
import com.sprint.otboo.clothing.entity.ClothesAttributeDef;
import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.clothing.entity.attribute.AttributeType;
import com.sprint.otboo.clothing.exception.ClothesExtractionException;
import com.sprint.otboo.clothing.repository.ClothesAttributeDefRepository;
import com.sprint.otboo.clothing.util.ClothesAttributeExtractor;
import com.sprint.otboo.clothing.util.ClothesAttributeExtractor.Attribute;
import com.sprint.otboo.common.storage.FileStorageService;
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
import org.springframework.web.multipart.MultipartFile;

@DisplayName("ClothesExtractor 단위 테스트")
public class ClothesExtractorTest {

    private MusinsaExtractor musinsaExtractor;
    private ZigzagExtractor zigzagExtractor;
    private TwentynineCMExtractor twentynineCmExtractor;
    private HiverExtractor hiverExtractor;
    private AblyExtractor ablyExtractor;
    private FourNineTenExtractor fourNineTenExtractor;
    private WConceptExtractor wConceptExtractor;
    private SsgExtractor ssgExtractor;

    @Mock
    private ClothesAttributeExtractor attributeExtractor;

    @Mock
    private ClothesAttributeDefRepository defRepository;

    @Mock
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        musinsaExtractor = new MusinsaExtractor(attributeExtractor, defRepository);
        zigzagExtractor = new ZigzagExtractor(attributeExtractor, defRepository);
        twentynineCmExtractor = new TwentynineCMExtractor(attributeExtractor, defRepository);
        hiverExtractor = new HiverExtractor(attributeExtractor, defRepository, fileStorageService);
        ablyExtractor = new AblyExtractor(attributeExtractor, defRepository, fileStorageService);
        fourNineTenExtractor = new FourNineTenExtractor(attributeExtractor, defRepository, fileStorageService);
        wConceptExtractor = new WConceptExtractor(attributeExtractor, defRepository);
        ssgExtractor = new SsgExtractor(attributeExtractor, defRepository);
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
        // given: 상품 URL, HTML 문서, 기본 정보, 속성, DB 정의
        String url = "https://www.musinsa.com/product/123";
        Document doc = mock(Document.class);

        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:title]"), eq("content"), anyString()))
            .thenReturn("후드 티셔츠");
        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:image]"), eq("content"), anyString()))
            .thenReturn("http://image.jpg");
        when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), eq(".breadcrumb a"), anyString()))
            .thenReturn("상의");

        ClothesAttributeExtractor.Attribute colorAttr = new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "RED");
        when(attributeExtractor.extractAttributes(eq(doc), eq("후드 티셔츠")))
            .thenReturn(List.of(colorAttr));

        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name("COLOR")
            .selectValues("RED,BLUE,BLACK")
            .build();
        when(defRepository.findByName("COLOR")).thenReturn(Optional.of(def));

        when(attributeExtractor.matchSelectableValue("RED", "RED,BLUE,BLACK")).thenReturn("RED");

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = musinsaExtractor.extract(url);

            // then: DTO 값 검증
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("후드 티셔츠");
            assertThat(result.imageUrl()).isEqualTo("http://image.jpg");
            assertThat(result.type()).isEqualTo(ClothesType.TOP);

            assertThat(result.attributes()).hasSize(1);
            ClothesAttributeDto attrDto = result.attributes().get(0);
            assertThat(attrDto.definitionId()).isEqualTo(defId);
            assertThat(attrDto.value()).isEqualTo("RED");
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
        // given: 상품 URL, HTML 문서, 기본 정보, 속성, DB 정의
        String url = "https://www.zigzag.kr/product/123";
        Document doc = mock(Document.class);

        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:title]"), eq("content"), anyString()))
            .thenReturn("에브리띵모던 후드 티셔츠");
        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:image]"), eq("content"), anyString()))
            .thenReturn("http://zigzag-image.jpg");
        when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), eq(".breadcrumb li a"), anyString()))
            .thenReturn("상의");

        ClothesAttributeExtractor.Attribute colorAttr = new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "RED");
        when(attributeExtractor.extractAttributes(eq(doc), eq("에브리띵모던 후드 티셔츠")))
            .thenReturn(List.of(colorAttr));

        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name("COLOR")
            .selectValues("RED,BLUE,BLACK")
            .build();
        when(defRepository.findByName("COLOR")).thenReturn(Optional.of(def));

        when(attributeExtractor.matchSelectableValue("RED", "RED,BLUE,BLACK")).thenReturn("RED");

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = zigzagExtractor.extract(url);

            // then: DTO 값 검증
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("에브리띵모던 후드 티셔츠");
            assertThat(result.imageUrl()).isEqualTo("http://zigzag-image.jpg");
            assertThat(result.type()).isEqualTo(ClothesType.TOP);

            assertThat(result.attributes()).hasSize(1);
            ClothesAttributeDto attrDto = result.attributes().get(0);
            assertThat(attrDto.definitionId()).isEqualTo(defId);
            assertThat(attrDto.value()).isEqualTo("RED");
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

    // ------------------ 29CM ------------------

    @Test
    void Twenty_nine_Cm_URL_지원_여부() {
        // given: 29CM 상품 URL
        String url = "https://www.29cm.co.kr/product/123";

        // when: supports 호출
        boolean result = twentynineCmExtractor.supports(url);

        // then: true 반환 확인
        assertThat(result).isTrue();
    }

    @Test
    void Twenty_nine_Cm_의상정보_정상추출() throws IOException {
        // given: URL, HTML 문서, 기본 정보, 속성, DB 정의, 내부 이미지 Mock
        String url = "https://www.29cm.co.kr/product/123";
        Document doc = mock(Document.class);

        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:title]"), eq("content"), anyString()))
            .thenReturn("슬림핏 셔츠");
        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:image]"), eq("content"), anyString()))
            .thenReturn("http://29cm-image.jpg");
        when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), eq(".breadcrumb li a"), anyString()))
            .thenReturn("상의");

        ClothesAttributeExtractor.Attribute colorAttr = new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "WHITE");
        when(attributeExtractor.extractAttributes(doc, "슬림핏 셔츠"))
            .thenReturn(List.of(colorAttr));

        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name("COLOR")
            .selectValues("WHITE,BLACK,BLUE")
            .build();
        when(defRepository.findByName("COLOR")).thenReturn(Optional.of(def));

        when(attributeExtractor.matchSelectableValue("WHITE", "WHITE,BLACK,BLUE")).thenReturn("WHITE");

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            Connection connWithUA = mock(Connection.class);

            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent("Mozilla/5.0")).thenReturn(connWithUA);
            when(connWithUA.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = twentynineCmExtractor.extract(url);

            // then: DTO 값 검증
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("슬림핏 셔츠");
            assertThat(result.imageUrl()).isEqualTo("http://29cm-image.jpg");
            assertThat(result.type()).isEqualTo(ClothesType.TOP);

            assertThat(result.attributes()).hasSize(1);
            ClothesAttributeDto attrDto = result.attributes().get(0);
            assertThat(attrDto.definitionId()).isEqualTo(defId);
            assertThat(attrDto.value()).isEqualTo("WHITE");
        }
    }

    @Test
    void Twenty_nine_Cm_DB정의없는속성_건너뛰기() throws IOException {
        // given: 상품 URL, 문서, 정의 없는 속성
        String url = "https://www.29cm.co.kr/product/123";

        Document doc = mock(Document.class);
        Element titleEl = mock(Element.class);
        when(titleEl.attr("content")).thenReturn("슬림핏 셔츠");
        when(doc.selectFirst("meta[property=og:title]")).thenReturn(titleEl);
        when(doc.selectFirst("meta[property=og:image]")).thenReturn(mock(Element.class));
        when(doc.select(".breadcrumb li a")).thenReturn(new Elements());

        when(attributeExtractor.extractAttributes(doc, "슬림핏 셔츠"))
            .thenReturn(List.of(new Attribute(AttributeType.COLOR, "UNKNOWN")));
        when(defRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            Connection connWithUA = mock(Connection.class); // userAgent 반환용

            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent("Mozilla/5.0")).thenReturn(connWithUA);
            when(connWithUA.get()).thenReturn(doc); // 실제 Document 반환

            // when: extract 호출
            ClothesDto result = twentynineCmExtractor.extract(url);

            // then: 정의 없는 속성 건너뛰기 확인
            assertThat(result.attributes()).isEmpty();
        }
    }

    @Test
    void Twenty_nine_Cm_IOException_발생시_예외변환() throws IOException {
        // given: 상품 URL, Jsoup 연결에서 IOException 발생
        String url = "https://www.29cm.co.kr/product/123";

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            Connection connWithUA = mock(Connection.class);

            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent("Mozilla/5.0")).thenReturn(connWithUA);

            // when & then: extract 호출 시 예외 변환 확인
            when(connWithUA.get()).thenThrow(new IOException("네트워크 오류"));
            assertThrows(ClothesExtractionException.class, () -> twentynineCmExtractor.extract(url));
        }
    }

    // ------------------ Hiver ------------------

    @Test
    void Hiver_URL_지원_여부() {
        // given: Hiver 상품 URL
        String url = "https://www.hiver.co.kr/product/123";

        // when: supports 호출
        boolean result = hiverExtractor.supports(url);

        // then: true 반환 확인
        assertThat(result).isTrue();
    }

    @Test
    void Hiver_의상정보_정상추출() throws IOException {
        // given: URL, HTML 문서, 기본 정보, 속성, DB 정의, 내부 이미지 업로드 Mock
        String url = "https://www.hiver.co.kr/product/123";
        Document doc = mock(Document.class);

        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:title]"), eq("content"), anyString()))
            .thenReturn("체크 셔츠");
        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:image]"), eq("content"), anyString()))
            .thenReturn("http://hiver-image.jpg");
        when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), eq(".breadcrumb li a"), anyString()))
            .thenReturn("상의");

        ClothesAttributeExtractor.Attribute colorAttr = new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "BLUE");
        when(attributeExtractor.extractAttributes(doc, "체크 셔츠"))
            .thenReturn(List.of(colorAttr));

        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name("COLOR")
            .selectValues("RED,BLUE,BLACK")
            .build();
        when(defRepository.findByName("COLOR")).thenReturn(Optional.of(def));
        when(attributeExtractor.matchSelectableValue("BLUE", "RED,BLUE,BLACK")).thenReturn("BLUE");

        MultipartFile mockFile = mock(MultipartFile.class);
        when(attributeExtractor.downloadImageAsMultipartFile("http://hiver-image.jpg"))
            .thenReturn(mockFile);
        when(fileStorageService.upload(mockFile))
            .thenReturn("http://internal-server/hiver-image.jpg");

        HiverExtractor extractor = new HiverExtractor(attributeExtractor, defRepository, fileStorageService);

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = extractor.extract(url);

            // then: DTO 값 검증
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("체크 셔츠");
            assertThat(result.imageUrl()).isEqualTo("http://internal-server/hiver-image.jpg");
            assertThat(result.type()).isEqualTo(ClothesType.TOP);

            assertThat(result.attributes()).hasSize(1);
            ClothesAttributeDto attrDto = result.attributes().get(0);
            assertThat(attrDto.definitionId()).isEqualTo(defId);
            assertThat(attrDto.value()).isEqualTo("BLUE");
        }
    }

    @Test
    void Hiver_DB정의없는속성_건너뛰기() throws IOException {
        // given: 상품 URL, HTML 문서, 정의 없는 속성
        String url = "https://www.hiver.co.kr/product/123";
        Document doc = mock(Document.class);

        Element titleEl = mock(Element.class);
        when(titleEl.attr("content")).thenReturn("체크 셔츠");
        when(doc.selectFirst("meta[property=og:title]")).thenReturn(titleEl);
        when(doc.selectFirst("meta[property=og:image]")).thenReturn(mock(Element.class));
        when(doc.select(".breadcrumb li a")).thenReturn(new Elements());

        when(attributeExtractor.extractAttributes(doc, "체크 셔츠"))
            .thenReturn(List.of(new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "UNKNOWN")));
        when(defRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            when(conn.userAgent("Mozilla/5.0")).thenReturn(conn);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = hiverExtractor.extract(url);

            // then: 정의 없는 속성 건너뛰기 확인
            assertThat(result.attributes()).isEmpty();
        }
    }

    @Test
    void Hiver_IOException_발생시_예외변환() throws IOException {
        // given: 상품 URL, Jsoup 연결에서 IOException 발생
        String url = "https://www.hiver.co.kr/product/123";

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent("Mozilla/5.0")).thenReturn(conn);
            when(conn.get()).thenThrow(new IOException("네트워크 오류"));

            // when & then: extract 호출 시 예외 변환 확인
            assertThrows(ClothesExtractionException.class, () -> hiverExtractor.extract(url));
        }
    }

    // ------------------ Ably ------------------

    @Test
    void Ably_URL_지원_여부() {
        // given: Ably 상품 URL
        String url = "https://www.a-bly.com/product/123";

        // when: supports 호출
        boolean result = ablyExtractor.supports(url);

        // then: true 반환 확인
        assertThat(result).isTrue();
    }

    @Test
    void Ably_의상정보_정상추출() throws IOException {
        // given: URL, HTML 문서, 기본 정보, 속성, DB 정의, 내부 이미지 업로드 Mock
        String url = "https://www.a-bly.com/product/123";
        Document doc = mock(Document.class);

        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:title]"), eq("content"), anyString()))
            .thenReturn("플로럴 원피스");
        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:image]"), eq("content"), anyString()))
            .thenReturn("http://ably-image.jpg");
        when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), eq(".breadcrumb a"), anyString()))
            .thenReturn("원피스");

        ClothesAttributeExtractor.Attribute colorAttr = new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "PINK");
        when(attributeExtractor.extractAttributes(doc, "플로럴 원피스"))
            .thenReturn(List.of(colorAttr));

        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name("COLOR")
            .selectValues("PINK,BLUE,WHITE")
            .build();
        when(defRepository.findByName("COLOR")).thenReturn(Optional.of(def));
        when(attributeExtractor.matchSelectableValue("PINK", "PINK,BLUE,WHITE")).thenReturn("PINK");

        MultipartFile mockFile = mock(MultipartFile.class);
        when(attributeExtractor.downloadImageAsMultipartFile("http://ably-image.jpg"))
            .thenReturn(mockFile);
        when(fileStorageService.upload(mockFile))
            .thenReturn("http://internal-server/ably-image.jpg");

        AblyExtractor extractor = new AblyExtractor(attributeExtractor, defRepository, fileStorageService);

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(conn);
            when(conn.header(anyString(), anyString())).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = extractor.extract(url);

            // then: DTO 값 검증
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("플로럴 원피스");
            assertThat(result.imageUrl()).isEqualTo("http://internal-server/ably-image.jpg");
            assertThat(result.type()).isEqualTo(ClothesType.DRESS); // 원피스 카테고리 반영
            assertThat(result.attributes()).hasSize(1);
            ClothesAttributeDto attrDto = result.attributes().get(0);
            assertThat(attrDto.definitionId()).isEqualTo(defId);
            assertThat(attrDto.value()).isEqualTo("PINK");
        }
    }


    @Test
    void Ably_DB정의없는속성_건너뛰기() throws IOException {
        // given: 상품 URL, HTML 문서, 정의 없는 속성 준비
        String url = "https://www.a-bly.com/product/123";
        Document doc = mock(Document.class);

        Element titleEl = mock(Element.class);
        when(titleEl.attr("content")).thenReturn("플로럴 원피스");
        when(doc.selectFirst("meta[property=og:title]")).thenReturn(titleEl);
        when(doc.selectFirst("meta[property=og:image]")).thenReturn(mock(Element.class));
        when(doc.select(".breadcrumb a")).thenReturn(new Elements());

        when(attributeExtractor.extractAttributes(doc, "플로럴 원피스"))
            .thenReturn(List.of(new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "UNKNOWN")));
        when(defRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(conn);
            when(conn.header(anyString(), anyString())).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = ablyExtractor.extract(url);

            // then: 정의 없는 속성 건너뛰기 확인
            assertThat(result.attributes()).isEmpty();
        }
    }

    @Test
    void Ably_IOException_발생시_예외변환() throws IOException {
        // given: 상품 URL, Jsoup 연결에서 IOException 발생 준비
        String url = "https://www.a-bly.com/product/123";

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(conn);
            when(conn.header(anyString(), anyString())).thenReturn(conn);
            when(conn.get()).thenThrow(new IOException("네트워크 오류"));

            // when & then: extract 호출 시 예외 변환 확인
            assertThrows(ClothesExtractionException.class, () -> ablyExtractor.extract(url));
        }
    }

    // ------------------ 4910 ------------------

    @Test
    void FourNineTen_URL_지원_여부() {
        // given: 4910 상품 URL
        String url = "https://www.4910.kr/product/123";

        // when: supports 호출
        boolean result = fourNineTenExtractor.supports(url);

        // then: true 반환 확인
        assertThat(result).isTrue();
    }

    @Test
    void FourNineTen_의상정보_정상추출() throws IOException {
        // given: URL, HTML 문서, 기본 정보, 속성, DB 정의, 내부 이미지 업로드 Mock
        String url = "https://www.4910.kr/product/123";
        Document doc = mock(Document.class);

        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:title]"), eq("content"), anyString()))
            .thenReturn("오버핏 후드티");
        when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:image]"), eq("content"), anyString()))
            .thenReturn("http://external-image.jpg");
        when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), eq(".breadcrumb a"), anyString()))
            .thenReturn("상의");

        ClothesAttributeExtractor.Attribute colorAttr = new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "RED");
        when(attributeExtractor.extractAttributes(doc, "오버핏 후드티"))
            .thenReturn(List.of(colorAttr));

        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name("COLOR")
            .selectValues("RED,BLUE,BLACK")
            .build();
        for (String dbName : ClothesAttributeExtractor.TYPE_TO_DB_NAMES.get(AttributeType.COLOR)) {
            when(defRepository.findByName(dbName)).thenReturn(Optional.of(def));
        }
        when(attributeExtractor.matchSelectableValue("RED", "RED,BLUE,BLACK")).thenReturn("RED");

        MultipartFile mockFile = mock(MultipartFile.class);
        when(attributeExtractor.downloadImageAsMultipartFile("http://external-image.jpg"))
            .thenReturn(mockFile);
        when(fileStorageService.upload(mockFile))
            .thenReturn("http://internal-server/4910-image.jpg");

        FourNineTenExtractor extractor = new FourNineTenExtractor(attributeExtractor, defRepository, fileStorageService);

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = extractor.extract(url);

            // then: DTO 값 검증
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("오버핏 후드티");
            assertThat(result.imageUrl()).isEqualTo("http://internal-server/4910-image.jpg");
            assertThat(result.type()).isEqualTo(ClothesType.TOP);
            assertThat(result.attributes()).hasSize(1);
            ClothesAttributeDto attrDto = result.attributes().get(0);
            assertThat(attrDto.definitionId()).isEqualTo(defId);
            assertThat(attrDto.value()).isEqualTo("RED");
        }
    }

    @Test
    void FourNineTen_DB정의없는속성_건너뛰기() throws IOException {
        // given: 상품 URL, HTML 문서, DB 정의 없는 속성 준비
        String url = "https://www.4910.kr/product/123";
        Document doc = mock(Document.class);

        Element titleEl = mock(Element.class);
        when(titleEl.attr("content")).thenReturn("오버핏 후드티");
        when(doc.selectFirst("meta[property=og:title]")).thenReturn(titleEl);
        when(doc.selectFirst("meta[property=og:image]")).thenReturn(mock(Element.class));
        when(doc.select(".breadcrumb a")).thenReturn(new Elements());

        when(attributeExtractor.extractAttributes(doc, "오버핏 후드티"))
            .thenReturn(List.of(new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "UNKNOWN")));
        when(defRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = fourNineTenExtractor.extract(url);

            // then: DB 정의 없는 속성 건너뛰기 확인
            assertThat(result.attributes()).isEmpty();
        }
    }

    @Test
    void FourNineTen_IOException_발생시_예외변환() throws IOException {
        // given: 상품 URL, Jsoup 연결에서 IOException 발생 준비
        String url = "https://www.4910.kr/product/123";

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(conn);
            when(conn.get()).thenThrow(new IOException("네트워크 오류"));

            // when & then: extract 호출 시 예외 변환 확인
            assertThrows(ClothesExtractionException.class, () -> fourNineTenExtractor.extract(url));
        }
    }

    // ------------------ W Concept ------------------

    @Test
    void WConcept_URL_지원_여부() {
        // given: W Concept 상품 URL
        String url = "https://www.wconcept.co.kr/product/123";

        // when: supports 호출
        boolean result = wConceptExtractor.supports(url);

        // then: true 반환 확인
        assertThat(result).isTrue();
    }

    @Test
    void WConcept_의상정보_정상추출() throws IOException {
        // given: 상품 URL, HTML 문서, 기본 정보, 속성, DB 정의
        String url = "https://www.wconcept.co.kr/product/123";
        Document doc = mock(Document.class);

        ClothesAttributeExtractor.Attribute colorAttr =
            new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "RED");

        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name("COLOR")
            .selectValues("RED,BLUE,BLACK")
            .build();

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            Connection userAgentConn = mock(Connection.class);

            // Jsoup.connect(url) -> conn 반환
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);

            // conn.userAgent(...) -> userAgentConn 반환
            when(conn.userAgent(anyString())).thenReturn(userAgentConn);

            // userAgentConn.get() -> doc 반환
            when(userAgentConn.get()).thenReturn(doc);

            // doc 기반 attributeExtractor stubbing
            when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[name=description]"), eq("content"), anyString()))
                .thenReturn("W Concept 후드 티셔츠");
            when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:image]"), eq("content"), anyString()))
                .thenReturn("http://wconcept-image.jpg");
            when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), eq(".breadcrumb li a"), anyString()))
                .thenReturn("상의");
            when(attributeExtractor.extractAttributes(eq(doc), eq("W Concept 후드 티셔츠")))
                .thenReturn(List.of(colorAttr));
            when(defRepository.findByName("COLOR")).thenReturn(Optional.of(def));
            when(attributeExtractor.matchSelectableValue("RED", "RED,BLUE,BLACK")).thenReturn("RED");

            // when: extract 호출
            ClothesDto result = wConceptExtractor.extract(url);

            // then: DTO 값 검증
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("W Concept 후드 티셔츠");
            assertThat(result.imageUrl()).isEqualTo("http://wconcept-image.jpg");
            assertThat(result.type()).isEqualTo(ClothesType.TOP);

            assertThat(result.attributes()).hasSize(1);
            ClothesAttributeDto attrDto = result.attributes().get(0);
            assertThat(attrDto.definitionId()).isEqualTo(defId);
            assertThat(attrDto.value()).isEqualTo("RED");
        }
    }

    @Test
    void WConcept_DB정의없는속성_건너뛰기() throws IOException {
        // given: 상품 URL, 문서, 정의 없는 속성
        String url = "https://www.wconcept.co.kr/product/123";
        Document doc = mock(Document.class);

        ClothesAttributeExtractor.Attribute unknownAttr =
            new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "UNKNOWN");

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            Connection userAgentConn = mock(Connection.class);

            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(userAgentConn);
            when(userAgentConn.get()).thenReturn(doc);

            when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[name=description]"), eq("content"), anyString()))
                .thenReturn("W Concept 후드 티셔츠");
            when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:image]"), eq("content"), anyString()))
                .thenReturn("http://wconcept-image.jpg");
            when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), eq(".breadcrumb li a"), anyString()))
                .thenReturn("상의");
            when(attributeExtractor.extractAttributes(eq(doc), eq("W Concept 후드 티셔츠")))
                .thenReturn(List.of(unknownAttr));
            when(defRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

            // when: extract 호출
            ClothesDto result = wConceptExtractor.extract(url);

            // then: 정의 없는 속성 건너뛰기 확인
            assertThat(result.attributes()).isEmpty();
        }
    }

    @Test
    void WConcept_IOException_발생시_예외변환() {
        // given: 상품 URL, Jsoup 연결에서 IOException 발생
        String url = "https://www.wconcept.co.kr/product/123";

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            Connection userAgentConn = mock(Connection.class);

            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(userAgentConn);
            try {
                when(userAgentConn.get()).thenThrow(new IOException("네트워크 오류"));
            } catch (IOException e) {
                throw new RuntimeException(e); // checked exception 처리
            }

            // when & then: extract 호출 시 ClothesExtractionException 확인
            assertThrows(ClothesExtractionException.class, () -> wConceptExtractor.extract(url));
        }
    }

    // ------------------ SSG ------------------

    @Test
    void Ssg_URL_지원_여부() {
        // given: SSG 상품 URL
        String url = "https://www.ssg.com/product/123";

        // when: supports 호출
        boolean result = ssgExtractor.supports(url);

        // then: true 반환 확인
        assertThat(result).isTrue();
    }

    @Test
    void Ssg_의상정보_정상추출() throws IOException {
        // given
        String url = "https://www.ssg.com/product/123";
        Document doc = mock(Document.class);

        ClothesAttributeExtractor.Attribute colorAttr =
            new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "RED");

        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name("COLOR")
            .selectValues("RED,BLUE,BLACK")
            .build();

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            Connection userAgentConn = mock(Connection.class);

            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(userAgentConn);
            when(userAgentConn.get()).thenReturn(doc);

            when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:title]"), eq("content"), anyString()))
                .thenReturn("SSG 후드티");
            when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:image]"), eq("content"), anyString()))
                .thenReturn("http://ssg-image.jpg");
            when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), eq(".breadcrumb li a"), anyString()))
                .thenReturn("상의");
            when(attributeExtractor.extractAttributes(eq(doc), eq("SSG 후드티")))
                .thenReturn(List.of(colorAttr));
            when(defRepository.findByName("COLOR")).thenReturn(Optional.of(def));
            when(attributeExtractor.matchSelectableValue("RED", "RED,BLUE,BLACK")).thenReturn("RED");

            // when
            ClothesDto result = ssgExtractor.extract(url);

            // then
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("SSG 후드티");
            assertThat(result.imageUrl()).isEqualTo("http://ssg-image.jpg");
            assertThat(result.type()).isEqualTo(ClothesType.TOP);

            assertThat(result.attributes()).hasSize(1);
            ClothesAttributeDto attrDto = result.attributes().get(0);
            assertThat(attrDto.definitionId()).isEqualTo(defId);
            assertThat(attrDto.value()).isEqualTo("RED");
        }
    }

    @Test
    void Ssg_DB정의없는속성_건너뛰기() throws IOException {
        // given
        String url = "https://www.ssg.com/product/123";
        Document doc = mock(Document.class);

        ClothesAttributeExtractor.Attribute unknownAttr =
            new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "UNKNOWN");

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            Connection userAgentConn = mock(Connection.class);

            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(userAgentConn);
            when(userAgentConn.get()).thenReturn(doc);

            when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:title]"), eq("content"), anyString()))
                .thenReturn("SSG 후드티");
            when(attributeExtractor.getAttrOrDefault(eq(doc), eq("meta[property=og:image]"), eq("content"), anyString()))
                .thenReturn("http://ssg-image.jpg");
            when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), eq(".breadcrumb li a"), anyString()))
                .thenReturn("상의");
            when(attributeExtractor.extractAttributes(eq(doc), eq("SSG 후드티")))
                .thenReturn(List.of(unknownAttr));
            when(defRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

            // when
            ClothesDto result = ssgExtractor.extract(url);

            // then
            assertThat(result.attributes()).isEmpty();
        }
    }

    @Test
    void Ssg_IOException_발생시_예외변환() {
        // given
        String url = "https://www.ssg.com/product/123";

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            Connection userAgentConn = mock(Connection.class);

            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(userAgentConn);
            try {
                when(userAgentConn.get()).thenThrow(new IOException("네트워크 오류"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // when & then
            assertThrows(ClothesExtractionException.class, () -> ssgExtractor.extract(url));
        }
    }
}