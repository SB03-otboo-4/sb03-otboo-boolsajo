package com.sprint.otboo.clothing.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

import com.sprint.otboo.clothing.entity.attribute.AttributeType;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ClothesAttributeExtractor 단위 테스트")
public class ClothesAttributeExtractorTest {

    private ClothesAttributeExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ClothesAttributeExtractor();
    }

    @Test
    void HTML_기반_속성_추출() {
        // given: HTML 내 색상/사이즈 요소 포함
        String html = """
            <html>
              <body>
                <ul class="product-info-color">
                  <li><button>black</button></li>
                  <li><button>white</button></li>
                </ul>
                <ul class="product-info-size">
                  <li><button>Medium</button></li>
                  <li><button>Large</button></li>
                </ul>
              </body>
            </html>
            """;
        Document doc = Jsoup.parse(html);

        // when: 속성 추출
        List<ClothesAttributeExtractor.Attribute> result = extractor.extractAttributes(doc, "");

        // then: 색상과 사이즈 속성이 Mapper 적용 후 포함
        assertThat(result)
            .extracting(ClothesAttributeExtractor.Attribute::type)
            .contains(AttributeType.COLOR, AttributeType.SIZE);
        assertThat(result)
            .extracting(ClothesAttributeExtractor.Attribute::value)
            .contains("Black", "White", "M", "L");
    }

    @Test
    void 텍스트_기반_속성_추출() {
        // given: 상품 설명 텍스트
        String text = "이 티셔츠는 red 컬러이며 XL 사이즈로 제공됩니다.";

        // when: 속성 추출
        List<ClothesAttributeExtractor.Attribute> result = extractor.extractAttributes(Jsoup.parse("<html></html>"), text);

        // then: COLOR, SIZE 속성이 Mapper 적용 후 포함
        assertThat(result)
            .extracting(ClothesAttributeExtractor.Attribute::type)
            .contains(AttributeType.COLOR, AttributeType.SIZE);
        assertThat(result)
            .extracting(ClothesAttributeExtractor.Attribute::value)
            .contains("Red", "XL");
    }

    @Test
    void JSON_LD_기반_속성_추출() {
        // given: JSON-LD 내 배열, offers 포함
        String html = """
            <html>
              <head>
                <script type="application/ld+json">
                  {
                    "@type": "Product",
                    "color": ["red","blue"],
                    "size": "Small",
                    "material": "cotton",
                    "offers": [
                      { "size": "LARGE", "material": "폴리에스터" },
                      { "color": "green", "size": "medium", "material": "코튼" }
                    ]
                  }
                </script>
              </head>
            </html>
            """;
        Document doc = Jsoup.parse(html);

        // when: 속성 추출
        List<ClothesAttributeExtractor.Attribute> result = extractor.extractAttributes(doc, "");

        // then: JSON-LD 내 배열, offers 속성 모두 Mapper 적용 후 포함
        assertThat(result)
            .extracting(ClothesAttributeExtractor.Attribute::type, ClothesAttributeExtractor.Attribute::value)
            .contains(
                tuple(AttributeType.COLOR, "Red"),
                tuple(AttributeType.COLOR, "Blue"),
                tuple(AttributeType.COLOR, "Green"),
                tuple(AttributeType.SIZE, "S"),
                tuple(AttributeType.SIZE, "L"),
                tuple(AttributeType.SIZE, "M"),
                tuple(AttributeType.MATERIAL, "Cotton"),
                tuple(AttributeType.MATERIAL, "Polyester")
            );
    }

    @Test
    void HTML_Text_JSON_LD_혼합_속성_추출() {
        // given: HTML, Text, JSON-LD 혼합
        String html = """
            <html>
              <head>
                <script type="application/ld+json">
                  { "@type": "Product", "color": "blue", "size": "small", "material": "폴리에스터" }
                </script>
              </head>
              <body>
                <ul class="product-info-color"><li><button>blue</button></li></ul>
              </body>
            </html>
            """;
        String text = "미디움 사이즈, 소재 코튼, 계절은 겨울, 두께는 Heavy";
        Document doc = Jsoup.parse(html);

        // when: 속성 추출
        List<ClothesAttributeExtractor.Attribute> result = extractor.extractAttributes(doc, text);

        // then: 중복 제거 후 모든 속성 포함
        assertThat(result)
            .extracting(ClothesAttributeExtractor.Attribute::type, ClothesAttributeExtractor.Attribute::value)
            .contains(
                tuple(AttributeType.COLOR, "Blue"),
                tuple(AttributeType.SIZE, "S"),
                tuple(AttributeType.SIZE, "M"),
                tuple(AttributeType.MATERIAL, "Polyester"),
                tuple(AttributeType.MATERIAL, "Cotton"),
                tuple(AttributeType.SEASON, "Winter"),
                tuple(AttributeType.THICKNESS, "Heavy")
            );

        // 중복 제거 확인
        long blueCount = result.stream()
            .filter(attr -> attr.type() == AttributeType.COLOR && attr.value().equals("Blue"))
            .count();
        assertThat(blueCount).isEqualTo(1);
    }

    @Test
    void 부분일치_완전일치_및_매핑불가_값_처리() {
        // given: 일부 매핑 가능, 일부 불가 값 포함
        String text = "컬러는 블랙이고 사이즈 XL, 소재는 코튼 혼방, 미지의색상 특이사이즈";

        // when: 속성 추출
        List<ClothesAttributeExtractor.Attribute> result = extractor.extractAttributes(Jsoup.parse("<html></html>"), text);

        // then: 매핑 가능한 속성만 추출
        assertThat(result)
            .extracting(ClothesAttributeExtractor.Attribute::type, ClothesAttributeExtractor.Attribute::value)
            .contains(
                tuple(AttributeType.COLOR, "Black"),
                tuple(AttributeType.SIZE, "XL"),
                tuple(AttributeType.MATERIAL, "CottonBlend")
            );

        // 매핑 불가 값은 무시
        assertThat(result.stream()
            .filter(attr -> attr.value().contains("미지") || attr.value().contains("특이")))
            .isEmpty();
    }

    @Test
    void HTML_요소_미존재_안전_처리() {
        // given: HTML 요소 없음
        String html = "<html><body></body></html>";
        Document doc = Jsoup.parse(html);

        // when: 속성 추출
        List<ClothesAttributeExtractor.Attribute> result = extractor.extractAttributes(doc, "");

        // then: NPE 없이 빈 리스트 반환
        assertThat(result).isEmpty();
    }

    @Test
    void 텍스트_다중_속성_혼합_추출() {
        // given: 여러 속성 포함 텍스트
        String text = "블랙 XL 면 혼방 여름용 두께 얇음";

        // when: 속성 추출
        List<ClothesAttributeExtractor.Attribute> result = extractor.extractAttributes(Jsoup.parse("<html></html>"), text);

        // then: 모든 속성 Mapper 적용 후 포함
        assertThat(result)
            .extracting(ClothesAttributeExtractor.Attribute::type, ClothesAttributeExtractor.Attribute::value)
            .contains(
                tuple(AttributeType.COLOR, "Black"),
                tuple(AttributeType.SIZE, "XL"),
                tuple(AttributeType.MATERIAL, "CottonBlend"),
                tuple(AttributeType.SEASON, "Summer"),
                tuple(AttributeType.THICKNESS, "Light")
            );
    }
}