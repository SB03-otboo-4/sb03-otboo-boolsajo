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

@DisplayName("GenericExtractor 단위 테스트")
public class ClothesExtractorTest {

    private GenericExtractor genericExtractor;

    @Mock
    private ClothesAttributeExtractor attributeExtractor;

    @Mock
    private ClothesAttributeDefRepository defRepository;

    @Mock
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        genericExtractor = new GenericExtractor(attributeExtractor, defRepository, fileStorageService);
    }

    // ------------------ 테스트 데이터 ------------------

    /**
     * <p>쇼핑몰 URL 및 테스트용 상품 데이터 정의</p>
     *
     * @param url 테스트 URL
     * @param expectedName 예상 상품명
     * @param externalImage 외부 이미지 URL
     * @param internalImage 내부 서버 업로드 이미지 URL (없으면 null)
     * @param type 예상 ClothesType
     * @param attrType 속성 타입
     * @param attrValue 속성 값
     * @param dbValues DB 정의 selectable 값
     */
    private static record TestShop(
        String url,
        String expectedName,
        String externalImage,
        String internalImage,
        ClothesType type,
        AttributeType attrType,
        String attrValue,
        String dbValues
    ) {}

    private final List<TestShop> shops = List.of(
        new TestShop("https://www.musinsa.com/product/123", "후드 티셔츠", "http://image.jpg", null, ClothesType.TOP, AttributeType.COLOR, "RED", "RED,BLUE,BLACK"),
        new TestShop("https://www.zigzag.kr/product/123", "에브리띵모던 후드 티셔츠", "http://zigzag-image.jpg", null, ClothesType.TOP, AttributeType.COLOR, "RED", "RED,BLUE,BLACK"),
        new TestShop("https://www.29cm.co.kr/product/123", "슬림핏 셔츠", "http://29cm-image.jpg", null, ClothesType.TOP, AttributeType.COLOR, "WHITE", "WHITE,BLACK,BLUE"),
        new TestShop("https://www.hiver.co.kr/product/123", "체크 셔츠", "http://hiver-image.jpg", "http://internal-server/hiver-image.jpg", ClothesType.TOP, AttributeType.COLOR, "BLUE", "RED,BLUE,BLACK"),
        new TestShop("https://www.a-bly.com/product/123", "플로럴 원피스", "http://ably-image.jpg", "http://internal-server/ably-image.jpg", ClothesType.DRESS, AttributeType.COLOR, "PINK", "PINK,BLUE,WHITE"),
        new TestShop("https://www.4910.kr/product/123", "오버핏 후드티", "http://external-image.jpg", "http://internal-server/4910-image.jpg", ClothesType.TOP, AttributeType.COLOR, "RED", "RED,BLUE,BLACK"),
        new TestShop("https://www.wconcept.co.kr/product/123", "W Concept 후드 티셔츠", "http://wconcept-image.jpg", null, ClothesType.TOP, AttributeType.COLOR, "RED", "RED,BLUE,BLACK"),
        new TestShop("https://www.ssg.com/product/123", "SSG 후드티", "http://ssg-image.jpg", null, ClothesType.TOP, AttributeType.COLOR, "RED", "RED,BLUE,BLACK"),
        new TestShop("https://www.brandi.co.kr/product/123", "린넨 셔츠", "http://brandi-image.jpg", "http://internal-server/brandi-image.jpg", ClothesType.TOP, AttributeType.COLOR, "WHITE", "WHITE,BEIGE,BLACK")
    );

    // ------------------ 헬퍼 메서드 ------------------

    /**
     * <p>해당 URL이 GenericExtractor에서 지원되는지 검증</p>
     *
     * <ul>
     *     <li>given: GenericExtractor, URL</li>
     *     <li>when: supports() 호출</li>
     *     <li>then: 지원 여부 true 확인</li>
     * </ul>
     */
    private void assertSupports(String url) {
        assertThat(genericExtractor.supports(url)).isTrue();
    }

    /**
     * <p>상품 정보를 정상적으로 추출하고 검증</p>
     *
     * <ul>
     *     <li>given: GenericExtractor, TestShop, Document/Mock 설정</li>
     *     <li>when: extract() 호출</li>
     *     <li>then: 상품명, 이미지, 타입, 속성이 올바르게 매핑되는지 검증</li>
     * </ul>
     */
    private void assertExtract(TestShop shop) throws IOException {
        // given: Document mock 및 기본 속성, 이미지, 카테고리 설정
        Document doc = mock(Document.class);
        when(attributeExtractor.getAttrOrDefault(eq(doc), anyString(), anyString(), anyString()))
            .thenReturn(shop.expectedName, shop.externalImage);
        when(attributeExtractor.getLastBreadcrumbOrDefault(eq(doc), anyString(), anyString()))
            .thenReturn(shop.type.name());

        // 속성 mock
        ClothesAttributeExtractor.Attribute attr =
            new ClothesAttributeExtractor.Attribute(shop.attrType, shop.attrValue);
        when(attributeExtractor.extractAttributes(eq(doc), eq(shop.expectedName)))
            .thenReturn(List.of(attr));

        // DB 정의 mock
        UUID defId = UUID.randomUUID();
        ClothesAttributeDef def = ClothesAttributeDef.builder()
            .id(defId)
            .name(shop.attrType.name())
            .selectValues(shop.dbValues)
            .build();
        when(defRepository.findByName(shop.attrType.name())).thenReturn(Optional.of(def));
        when(attributeExtractor.matchSelectableValue(shop.attrValue, shop.dbValues)).thenReturn(shop.attrValue);

        // 내부 이미지 업로드 mock
        if (shop.internalImage != null) {
            MultipartFile mockFile = mock(MultipartFile.class);
            when(attributeExtractor.downloadImageAsMultipartFile(shop.externalImage)).thenReturn(mockFile);
            when(fileStorageService.upload(mockFile)).thenReturn(shop.internalImage);
        }

        // when: GenericExtractor.extract() 호출
        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(shop.url)).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            ClothesDto result = genericExtractor.extract(shop.url);

            // then: 결과 DTO 검증
            assertThat(result.name()).isEqualTo(shop.expectedName);
            assertThat(result.imageUrl()).isEqualTo(shop.internalImage != null ? shop.internalImage : shop.externalImage);
            assertThat(result.type()).isEqualTo(shop.type);

            assertThat(result.attributes()).hasSize(1);
            ClothesAttributeDto attrDto = result.attributes().get(0);
            assertThat(attrDto.value()).isEqualTo(shop.attrValue);
        }
    }

    /**
     * <p>DB 정의가 없는 속성은 건너뛰는지 검증</p>
     *
     * <ul>
     *     <li>given: TestShop, 정의되지 않은 속성</li>
     *     <li>when: extract() 호출</li>
     *     <li>then: attributes가 비어있는지 확인</li>
     * </ul>
     */
    private void assertSkipUndefinedAttributes(TestShop shop) throws IOException {
        // given: Document mock, 정의되지 않은 속성 설정
        Document doc = mock(Document.class);
        when(attributeExtractor.extractAttributes(eq(doc), eq(shop.expectedName)))
            .thenReturn(List.of(new ClothesAttributeExtractor.Attribute(AttributeType.COLOR, "UNKNOWN")));
        when(defRepository.findByName("UNKNOWN")).thenReturn(Optional.empty());

        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {

            // Jsoup 연결 mock
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(shop.url)).thenReturn(conn);
            when(conn.get()).thenReturn(doc);

            // when: extract 호출
            ClothesDto result = genericExtractor.extract(shop.url);

            // then: 속성이 비어있는지 검증
            assertThat(result.attributes()).isEmpty();
        }
    }

    /**
     * <p>extract() 호출 시 IOException 발생 시 ClothesExtractionException으로 변환되는지 검증</p>
     *
     * <ul>
     *     <li>given: URL, IOException 상황</li>
     *     <li>when: extract() 호출</li>
     *     <li>then: ClothesExtractionException 발생</li>
     * </ul>
     */
    private void assertIOExceptionExtractor(String url) {
        try (MockedStatic<Jsoup> jsoupStatic = mockStatic(Jsoup.class)) {
            // given: Jsoup 연결 mock 및 IOException 설정
            Connection conn = mock(Connection.class);
            jsoupStatic.when(() -> Jsoup.connect(url)).thenReturn(conn);
            try {
                when(conn.get()).thenThrow(new IOException("네트워크 오류"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // when & then: extract 호출 시 예외 발생 검증
            assertThrows(ClothesExtractionException.class, () -> genericExtractor.extract(url));
        }
    }

    // ------------------ 반복 테스트 ------------------

    @Test
    void 모든쇼핑몰_URL지원_검증() {
        for (TestShop shop : shops) {
            assertSupports(shop.url);
        }
    }

    @Test
    void 모든쇼핑몰_정상추출_검증() throws IOException {
        for (TestShop shop : shops) {
            assertExtract(shop);
        }
    }

    @Test
    void 모든쇼핑몰_DB정의없는속성_건너뛰기_검증() throws IOException {
        for (TestShop shop : shops) {
            assertSkipUndefinedAttributes(shop);
        }
    }

    @Test
    void 모든쇼핑몰_IOException_예외변환_검증() {
        for (TestShop shop : shops) {
            assertIOExceptionExtractor(shop.url);
        }
    }
}