package com.sprint.otboo.clothing.scraper;


import com.sprint.otboo.clothing.dto.data.ClothesDto;

public interface ClothesExtractor {

    boolean supports(String url);

    ClothesDto extract(String url);
}
