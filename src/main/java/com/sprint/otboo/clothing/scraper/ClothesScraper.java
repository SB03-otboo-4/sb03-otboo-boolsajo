package com.sprint.otboo.clothing.scraper;

import com.sprint.otboo.clothing.dto.data.ScrapedClothesDto;

public abstract class ClothesScraper {

    // 주어진 URL에서 의상 정보 추출
    public abstract ScrapedClothesDto extract(String url);

    // URL이 해당 사이트 소속인지 확인
    public abstract boolean supports(String url);

}
