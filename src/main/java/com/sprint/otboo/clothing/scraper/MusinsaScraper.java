package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ScrapedClothesAttributeDto;
import com.sprint.otboo.clothing.dto.data.ScrapedClothesDto;
import java.io.IOException;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class MusinsaScraper extends ClothesScraper {

    @Override
    public boolean supports(String url) {
        return url.contains("musinsa.com");
    }

    @Override
    public ScrapedClothesDto extract(String url) {
        try {
            Document doc = Jsoup.connect(url).get();

            String name = doc.selectFirst("h1.product_title").text();
            String imageUrl = doc.selectFirst("img.product_image").attr("src");
            String type = doc.selectFirst("span.category").text();

            // 속성 예시
            List<ScrapedClothesAttributeDto> attributes = List.of(
                new ScrapedClothesAttributeDto("색상", List.of("Red", "Blue"), doc.selectFirst("span.color").text()),
                new ScrapedClothesAttributeDto("사이즈", List.of("S", "M", "L"), doc.selectFirst("span.size").text())
            );

            return new ScrapedClothesDto(name, imageUrl, type, attributes);

        } catch (IOException e) {
            throw new RuntimeException("무신사 스크래핑 실패", e);
        }
    }
}
