package com.sprint.otboo.clothing.service;

import com.sprint.otboo.clothing.dto.data.ScrapedClothesDto;

public interface ClothesScraperService {

    ScrapedClothesDto extractByUrl(String url);
}
