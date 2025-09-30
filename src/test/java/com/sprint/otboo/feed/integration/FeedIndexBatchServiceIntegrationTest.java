package com.sprint.otboo.feed.integration;

import static org.mockito.Mockito.times;

import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.feedsearch.batch.FeedIndexBatchService;
import com.sprint.otboo.feedsearch.batch.FeedIndexer;
import com.sprint.otboo.fixture.FeedFixture;
import com.sprint.otboo.fixture.UserFixture;
import com.sprint.otboo.fixture.WeatherFixture;
import com.sprint.otboo.fixture.WeatherLocationFixture;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.entity.WeatherLocation;
import com.sprint.otboo.weather.repository.WeatherLocationRepository;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class FeedIndexBatchServiceIntegrationTest {

    @Autowired
    FeedRepository feedRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    WeatherRepository weatherRepository;
    @Autowired
    WeatherLocationRepository weatherLocationRepository;

    @Autowired
    FeedIndexBatchService batchService;

    @MockitoBean
    FeedIndexer feedIndexer;

    @Test
    void 삭제되지_않은_피드만_키셋_순서대로_업서트한다() throws Exception {
        // Given
        User user = userRepository.save(UserFixture.createUserWithDefault());
        WeatherLocation loc = weatherLocationRepository.save(
            WeatherLocationFixture.createLocationWithDefault());
        Weather weather = weatherRepository.save(WeatherFixture.createWeatherWithDefault(loc));

        Feed a = feedRepository.save(
            FeedFixture.createWithContent(user, weather, Instant.parse("2025-09-01T00:00:00Z"),
                "A"));
        Feed b = feedRepository.save(
            FeedFixture.createWithContent(user, weather, Instant.parse("2025-09-01T00:00:00Z"),
                "B"));
        Feed c = feedRepository.save(
            FeedFixture.createWithContent(user, weather, Instant.parse("2025-09-02T00:00:00Z"),
                "C"));
        // 논리 삭제
        c.softDelete();
        feedRepository.save(c);

        // When
        batchService.reindex();

        // Then
        Mockito.verify(feedIndexer, Mockito.atLeastOnce()).bulkUpsert(Mockito.argThat(list ->
            list.size() == 2 &&
                list.get(0).content().equals("A") &&
                list.get(1).content().equals("B")
        ));
        Mockito.verify(feedIndexer).refresh();
    }

    @Test
    void 문서가_없으면_bulkUpsert는_호출하지_않고_refresh만_호출한다() throws Exception {
        // Given: DB 비어있음

        // When
        batchService.reindex();

        // Then
        Mockito.verify(feedIndexer, Mockito.never()).bulkUpsert(Mockito.anyList());
        Mockito.verify(feedIndexer, times(1)).refresh();
    }
}
