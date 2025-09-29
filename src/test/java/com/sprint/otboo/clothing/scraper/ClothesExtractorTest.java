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
import com.sprint.otboo.common.storage.FileStorageService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
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
    private BrandiExtractor brandiExtractor;

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
        brandiExtractor = new BrandiExtractor(attributeExtractor, defRepository, fileStorageService);
    }

    // ------------------ 헬퍼 메서드 ------------------

    /**
     * extractor에서 지원되는지 검증
     *
     * <ul>
     *     <li>given: extractor, URL</li>
     *     <li>when: supports 메서드 호출</li>
     *     <li>then: URL 지원 여부가 true인지 검증</li>
     */
    private void assertSupports(ClothesExtractor extractor, String url) {
        assertThat(extractor.supports(url)).isTrue();
    }

    /**
     * 상품 정보를 정상적으로 추출하고 검증
     *
     * <ul>
     *     <li>given: extractor, URL, 예상 상품명/이미지/속성, DB 정의 값</li>
     *     <li>when: extract 메서드 호출</li>
     *     <li>then: 상품 정보와 속성이 올바르게 추출되는지 검증</li>
     */
    private void assertExtractWithOptionalImage(
        ClothesExtractor extractor,
        String url,
        String expectedName,
        String externalImage,
        String internalImage, // null이면 업로드 없음
        ClothesType expectedType,
        AttributeType attrType,
        String attrValue,
        String dbValues
    ) throws IOException {
        // given: Jsoup Document mock, attributeExtractor/defRepository Mock 설정
        Document doc = mock(Document.class);
        when(attributeExtractor.getAttrOrDefault(eq(doc), anyString(), anyString(), anyString()))
            .thenReturn(expectedName, externalImage);
        when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), anyString(), anyString()))
            .thenReturn(expectedType.name());

        ClothesAttributeExtractor.Attribute attr =
            new ClothesAttributeExtractor.Attribute(attrType, attrValue);
        when(attributeExtractor.extractAttributes(eq(doc), eq(expectedName)))
            .thenReturn(List.of(attr));

        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name(attrType.name())
            .selectValues(dbValues)
            .build();
        when(defRepository.findByName(attrType.name())).thenReturn(Optional.of(def));
        when(attributeExtractor.matchSelectableValue(attrValue, dbValues)).thenReturn(attrValue);

        // 내부 서버 업로드 있는 경우
        if (internalImage != null) {
            MultipartFile mockFile = mock(MultipartFile.class);
            when(attributeExtractor.downloadImageAsMultipartFile(externalImage)).thenReturn(mockFile);
            when(fileStorageService.upload(mockFile)).thenReturn(internalImage);
        }

        // when: extract 호출
        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(conn);
            if (extractor instanceof AblyExtractor || extractor instanceof BrandiExtractor) {
                when(conn.header(anyString(), anyString())).thenReturn(conn);
            }
            when(conn.get()).thenReturn(doc);

            ClothesDto result = extractor.extract(url);

            // then: 결과 검증
            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo(expectedName);
            assertThat(result.imageUrl()).isEqualTo(internalImage != null ? internalImage : externalImage);
            assertThat(result.type()).isEqualTo(expectedType);

            assertThat(result.attributes()).hasSize(1);
            ClothesAttributeDto attrDto = result.attributes().get(0);
            assertThat(attrDto.definitionId()).isEqualTo(defId);
            assertThat(attrDto.value()).isEqualTo(attrValue);
        }
    }

    /**
     * DB 정의 없는 속성은 건너뛰는지 검증
     *
     * <ul>
     *     <li>given: extractor, URL, 상품명, 정의되지 않은 속성</li>
     *     <li>when: extract 호출</li>
     *     <li>then: DB 정의 없는 속성은 건너뛰고 attributes가 비어있는지 검증</li>
     */
    private void assertSkipUndefinedAttributesWithOptionalImage(
        ClothesExtractor extractor, String url, String productName,
        String externalImage, String internalImage
    ) throws IOException {
        // given: Document mock, attributeExtractor/defRepository Mock 설정
        Document doc = mock(Document.class);
        when(attributeExtractor.extractAttributes(eq(doc), eq(productName)))
            .thenReturn(List.of(new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "UNKNOWN")));
        when(defRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        // when: extract 호출
        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(conn);
            if (extractor instanceof AblyExtractor || extractor instanceof BrandiExtractor) {
                when(conn.header(anyString(), anyString())).thenReturn(conn);
            }
            when(conn.get()).thenReturn(doc);

            ClothesDto result = extractor.extract(url);

            // then: attributes가 비어있는지 검증
            assertThat(result.attributes()).isEmpty();
        }
    }

    /**
     * extract 호출 시 IOException 발생 시 예외 변환 확인
     *
     * <ul>
     *     <li>given: extractor, URL, IOException 상황</li>
     *     <li>when: extract 호출</li>
     *     <li>then: IOException 발생 시 ClothesExtractionException으로 변환되는지 검증</li>
     */
    private void assertIOExceptionExtractor(ClothesExtractor extractor, String url) {
        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            when(conn.userAgent(anyString())).thenReturn(conn);
            if (extractor instanceof AblyExtractor || extractor instanceof BrandiExtractor) {
                when(conn.header(anyString(), anyString())).thenReturn(conn);
            }
            try {
                when(conn.get()).thenThrow(new IOException("네트워크 오류"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // then: ClothesExtractionException 발생
            assertThrows(ClothesExtractionException.class, () -> extractor.extract(url));
        }
    }

    // ------------------ Musinsa ------------------

    @Test
    void 무신사_URL_지원_여부() {
        assertSupports(musinsaExtractor, "https://www.musinsa.com/product/123");
    }

    @Test
    void Musinsa_의상정보_정상추출() throws IOException {
        assertExtractWithOptionalImage(musinsaExtractor, "https://www.musinsa.com/product/123",
            "후드 티셔츠", "http://image.jpg", null, ClothesType.TOP, AttributeType.COLOR, "RED", "RED,BLUE,BLACK");
    }

    @Test
    void Musinsa_DB정의없는속성_건너뛰기() throws IOException {
        assertSkipUndefinedAttributesWithOptionalImage(musinsaExtractor, "https://www.musinsa.com/product/123",
            "후드 티셔츠", "http://image.jpg", null);    }

    @Test
    void Musinsa_IOException_발생시_예외변환() throws IOException {
        assertIOExceptionExtractor(musinsaExtractor, "https://www.musinsa.com/product/123");
    }

    // ------------------ Zigzag ------------------

    @Test
    void 지그재그_URL_지원_여부() {
        assertSupports(zigzagExtractor, "https://www.zigzag.kr/product/123");
    }

    @Test
    void Zigzag_의상정보_정상추출() throws IOException {
        assertExtractWithOptionalImage(zigzagExtractor, "https://www.zigzag.kr/product/123",
            "에브리띵모던 후드 티셔츠", "http://zigzag-image.jpg", null, ClothesType.TOP, AttributeType.COLOR, "RED", "RED,BLUE,BLACK");
    }

    @Test
    void Zigzag_DB정의없는속성_건너뛰기() throws IOException {
        assertSkipUndefinedAttributesWithOptionalImage(zigzagExtractor, "https://www.zigzag.kr/product/123",
            "에브리띵모던 후드 티셔츠", "http://zigzag-image.jpg", null);
    }

    @Test
    void Zigzag_IOException_발생시_예외변환() throws IOException {
        assertIOExceptionExtractor(zigzagExtractor, "https://www.zigzag.kr/product/123");
    }

    // ------------------ 29CM ------------------

    @Test
    void Twenty_nine_Cm_URL_지원_여부() {
        assertSupports(twentynineCmExtractor, "https://www.29cm.co.kr/product/123");    }

    @Test
    void Twenty_nine_Cm_의상정보_정상추출() throws IOException {
        assertExtractWithOptionalImage(twentynineCmExtractor, "https://www.29cm.co.kr/product/123",
            "슬림핏 셔츠", "http://29cm-image.jpg", null, ClothesType.TOP, AttributeType.COLOR, "WHITE", "WHITE,BLACK,BLUE");
    }

    @Test
    void Twenty_nine_Cm_DB정의없는속성_건너뛰기() throws IOException {
        assertSkipUndefinedAttributesWithOptionalImage(twentynineCmExtractor, "https://www.29cm.co.kr/product/123",
            "슬림핏 셔츠", "http://29cm-image.jpg", null);
    }

    @Test
    void Twenty_nine_Cm_IOException_발생시_예외변환() throws IOException {
        assertIOExceptionExtractor(twentynineCmExtractor, "https://www.29cm.co.kr/product/123");
    }

    // ------------------ Hiver ------------------

    @Test
    void Hiver_URL_지원_여부() {
        assertSupports(hiverExtractor, "https://www.hiver.co.kr/product/123");
    }

    @Test
    void Hiver_의상정보_정상추출() throws IOException {
        assertExtractWithOptionalImage(hiverExtractor, "https://www.hiver.co.kr/product/123",
            "체크 셔츠", "http://hiver-image.jpg", "http://internal-server/hiver-image.jpg",
            ClothesType.TOP, AttributeType.COLOR, "BLUE", "RED,BLUE,BLACK");
    }

    @Test
    void Hiver_DB정의없는속성_건너뛰기() throws IOException {
        assertSkipUndefinedAttributesWithOptionalImage(hiverExtractor, "https://www.hiver.co.kr/product/123",
            "체크 셔츠", "http://hiver-image.jpg", "http://internal-server/hiver-image.jpg");    }

    @Test
    void Hiver_IOException_발생시_예외변환() throws IOException {
        assertIOExceptionExtractor(hiverExtractor, "https://www.hiver.co.kr/product/123");
    }

    // ------------------ Ably ------------------

    @Test
    void Ably_URL_지원_여부() {
        assertSupports(ablyExtractor, "https://www.a-bly.com/product/123");
    }

    @Test
    void Ably_의상정보_정상추출() throws IOException {
        assertExtractWithOptionalImage(ablyExtractor, "https://www.a-bly.com/product/123",
            "플로럴 원피스", "http://ably-image.jpg", "http://internal-server/ably-image.jpg",
            ClothesType.DRESS, AttributeType.COLOR, "PINK", "PINK,BLUE,WHITE");
    }

    @Test
    void Ably_DB정의없는속성_건너뛰기() throws IOException {
        assertSkipUndefinedAttributesWithOptionalImage(ablyExtractor, "https://www.a-bly.com/product/123",
            "플로럴 원피스", "http://ably-image.jpg", "http://internal-server/ably-image.jpg");
    }

    @Test
    void Ably_IOException_발생시_예외변환() throws IOException {
        assertIOExceptionExtractor(ablyExtractor, "https://www.a-bly.com/product/123");
    }

    // ------------------ 4910 ------------------

    @Test
    void FourNineTen_URL_지원_여부() {
        assertSupports(fourNineTenExtractor, "https://www.4910.kr/product/123");
    }

    @Test
    void FourNineTen_의상정보_정상추출() throws IOException {
        assertExtractWithOptionalImage(fourNineTenExtractor, "https://www.4910.kr/product/123",
            "오버핏 후드티", "http://external-image.jpg", "http://internal-server/4910-image.jpg",
            ClothesType.TOP, AttributeType.COLOR, "RED", "RED,BLUE,BLACK");
    }

    @Test
    void FourNineTen_DB정의없는속성_건너뛰기() throws IOException {
        assertSkipUndefinedAttributesWithOptionalImage(fourNineTenExtractor, "https://www.4910.kr/product/123",
            "오버핏 후드티", "http://external-image.jpg", "http://internal-server/4910-image.jpg");
    }

    @Test
    void FourNineTen_IOException_발생시_예외변환() throws IOException {
        assertIOExceptionExtractor(fourNineTenExtractor, "https://www.4910.kr/product/123");
    }

    // ------------------ W Concept ------------------

    @Test
    void WConcept_URL_지원_여부() {
        assertSupports(wConceptExtractor, "https://www.wconcept.co.kr/product/123");
    }

    @Test
    void WConcept_의상정보_정상추출() throws IOException {
        assertExtractWithOptionalImage(wConceptExtractor, "https://www.wconcept.co.kr/product/123",
            "W Concept 후드 티셔츠", "http://wconcept-image.jpg", null,
            ClothesType.TOP, AttributeType.COLOR, "RED", "RED,BLUE,BLACK");
    }

    @Test
    void WConcept_DB정의없는속성_건너뛰기() throws IOException {
        assertSkipUndefinedAttributesWithOptionalImage(wConceptExtractor, "https://www.wconcept.co.kr/product/123",
            "W Concept 후드 티셔츠", "http://wconcept-image.jpg", null);
    }

    @Test
    void WConcept_IOException_발생시_예외변환() {
        assertIOExceptionExtractor(wConceptExtractor, "https://www.wconcept.co.kr/product/123");
    }

    // ------------------ SSG ------------------

    @Test
    void Ssg_URL_지원_여부() {
        assertSupports(ssgExtractor, "https://www.ssg.com/product/123");
    }

    @Test
    void Ssg_의상정보_정상추출() throws IOException {
        assertExtractWithOptionalImage(ssgExtractor, "https://www.ssg.com/product/123",
            "SSG 후드티", "http://ssg-image.jpg", null,
            ClothesType.TOP, AttributeType.COLOR, "RED", "RED,BLUE,BLACK");
    }

    @Test
    void Ssg_DB정의없는속성_건너뛰기() throws IOException {
        assertSkipUndefinedAttributesWithOptionalImage(ssgExtractor, "https://www.ssg.com/product/123",
            "SSG 후드티", "http://ssg-image.jpg", null);
    }

    @Test
    void Ssg_IOException_발생시_예외변환() {
        assertIOExceptionExtractor(ssgExtractor, "https://www.ssg.com/product/123");
    }

    // ------------------ Brandi ------------------

    @Test
    void Brandi_URL_지원_여부() {
        assertSupports(brandiExtractor, "https://www.brandi.co.kr/product/123");
    }

    @Test
    void Brandi_의상정보_정상추출() throws IOException {
        assertExtractWithOptionalImage(brandiExtractor, "https://www.brandi.co.kr/product/123",
            "린넨 셔츠", "http://brandi-image.jpg", "http://internal-server/brandi-image.jpg",
            ClothesType.TOP, AttributeType.COLOR, "WHITE", "WHITE,BEIGE,BLACK");
    }

    @Test
    void Brandi_DB정의없는속성_건너뛰기() throws IOException {
        assertSkipUndefinedAttributesWithOptionalImage(brandiExtractor, "https://www.brandi.co.kr/product/123",
            "린넨 셔츠", "http://brandi-image.jpg", "http://internal-server/brandi-image.jpg");
    }

    @Test
    void Brandi_IOException_발생시_예외변환() throws IOException {
        assertIOExceptionExtractor(brandiExtractor, "https://www.brandi.co.kr/product/123");
    }
}