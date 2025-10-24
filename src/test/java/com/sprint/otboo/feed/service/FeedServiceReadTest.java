package com.sprint.otboo.feed.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.sprint.otboo.clothing.entity.ClothesType;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.feedsearch.repository.FeedSearchRepository;
import com.sprint.otboo.fixture.FeedFixture;
import com.sprint.otboo.fixture.UserFixture;
import com.sprint.otboo.fixture.WeatherFixture;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedService 조회 테스트")
class FeedServiceReadTest {

    @Mock
    FeedRepository feedRepository;
    @Mock
    FeedSearchRepository esFeedRepository;
    @Mock
    FeedMapper feedMapper;
    @InjectMocks
    FeedServiceImpl feedService;

    private static final int LIMIT = 10;
    private static final String SORT_BY = "createdAt";
    private static final String SORT_DIR = "DESCENDING";
    private static final Instant FIXED_NOW = Instant.parse("2025-09-18T00:00:00Z");

    private UUID authorId;
    private UUID weatherId;
    private UUID clothesId;

    private User author;
    private Weather weather;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        weatherId = UUID.randomUUID();
        clothesId = UUID.randomUUID();

        author = UserFixture.create(authorId, "홍길동", "profile.png");
        weather = WeatherFixture.create(weatherId);
    }

    private Feed newFeed(UUID id, String content) {
        return FeedFixture.createEntity(id, author, weather, content, FIXED_NOW, FIXED_NOW);
    }

    private Feed newFeedAt(UUID id, String content, Instant createdAt) {
        return FeedFixture.createEntity(id, author, weather, content, createdAt, createdAt);
    }

    private FeedDto newDtoFrom(Feed feed,
        String skyName,
        String precipitationName,
        long likeCount,
        int commentCount) {
        return FeedFixture.createDto(
            feed.getId(), FIXED_NOW, FIXED_NOW,
            author.getId(), "홍길동", "profile.png",
            weather.getId(), skyName,
            precipitationName, 0.0, 0.0,
            25.0, -1.0, 20.0, 27.0,
            clothesId, "셔츠", "image.png", ClothesType.TOP,
            feed.getContent(), likeCount, commentCount, false
        );
    }

    @Nested
    @DisplayName("기본 피드 조회")
    class BasicFeedReadTests {

        @Test
        void 피드를_조회하면_DTO가_반환된다() {
            // Given
            UUID feedId = UUID.randomUUID();
            Feed feed = newFeed(feedId, "오늘의 코디");
            FeedDto dto = newDtoFrom(feed, "맑음", "비", 10L, 2);

            CursorPageResponse<UUID> idPage = new CursorPageResponse<>(
                List.of(feedId),   // data
                null,              // nextCursor
                null,              // nextIdAfter
                false,             // hasNext
                1L,                // totalCount
                SORT_BY,
                SORT_DIR
            );

            given(esFeedRepository.searchIds(
                null, null, LIMIT, SORT_BY, SORT_DIR,
                null, null, null, null
            )).willReturn(idPage);

            given(feedRepository.findAllById(List.of(feedId))).willReturn(List.of(feed));

            given(esFeedRepository.countByFilters(null, null, null, null)).willReturn(1L);

            given(feedMapper.toDto(feed)).willReturn(dto);

            // When
            CursorPageResponse<FeedDto> result =
                feedService.getFeeds(null, null, LIMIT, SORT_BY, SORT_DIR, null, null, null, null);

            // Then
            assertThat(result.totalCount()).isEqualTo(1L);
            assertThat(result.data()).containsExactly(dto);

            then(esFeedRepository).should()
                .searchIds(null, null, LIMIT, SORT_BY, SORT_DIR, null, null, null, null);
            then(feedRepository).should().findAllById(List.of(feedId));
            then(esFeedRepository).should().countByFilters(null, null, null, null);
            then(feedMapper).should().toDto(feed);

            then(esFeedRepository).shouldHaveNoMoreInteractions();
            then(feedRepository).shouldHaveNoMoreInteractions();
            then(feedMapper).shouldHaveNoMoreInteractions();
        }

    }

    @Nested
    @DisplayName("피드 조회 필터링 테스트")
    class FeedReadFilteringTests {

        @Test
        void SkyStatus_CLEAR_이면_매칭되는_DTO만_반환된다() {
            // Given
            SkyStatus filterStatus = SkyStatus.CLEAR;

            Feed feed = newFeed(UUID.randomUUID(), "맑은날 코디");
            FeedDto clearDto = newDtoFrom(feed, SkyStatus.CLEAR.name(), "없음", 3L, 1);

            CursorPageResponse<UUID> idPage = new CursorPageResponse<>(
                List.of(feed.getId()), null, null, false, 1L, SORT_BY, SORT_DIR
            );

            given(esFeedRepository.searchIds(
                null, null, LIMIT, SORT_BY, SORT_DIR,
                null, filterStatus, null, null
            )).willReturn(idPage);

            given(feedRepository.findAllById(List.of(feed.getId()))).willReturn(List.of(feed));
            given(esFeedRepository.countByFilters(null, filterStatus, null, null)).willReturn(1L);
            given(feedMapper.toDto(feed)).willReturn(clearDto);

            // When
            CursorPageResponse<FeedDto> result =
                feedService.getFeeds(null, null, LIMIT, SORT_BY, SORT_DIR, null, filterStatus, null,
                    null);

            // Then
            assertThat(result.data()).containsExactly(clearDto);
            assertThat(result.totalCount()).isEqualTo(1L);
        }

        @ParameterizedTest(name = "CLEAR 외 상태: {0}")
        @EnumSource(value = SkyStatus.class, names = {"CLEAR"}, mode = EnumSource.Mode.EXCLUDE)
        void SkyStatus_CLEAR_외에는_빈_결과를_반환한다(SkyStatus filterStatus) {
            // Given
            CursorPageResponse<UUID> idPage = new CursorPageResponse<>(
                List.of(), null, null, false, 0L, SORT_BY, SORT_DIR
            );

            given(esFeedRepository.searchIds(
                null, null, LIMIT, SORT_BY, SORT_DIR,
                null, filterStatus, null, null
            )).willReturn(idPage);

            given(esFeedRepository.countByFilters(null, filterStatus, null, null)).willReturn(0L);

            // When
            CursorPageResponse<FeedDto> result =
                feedService.getFeeds(null, null, LIMIT, SORT_BY, SORT_DIR, null, filterStatus, null,
                    null);

            // Then
            assertThat(result.data()).isEmpty();
            assertThat(result.totalCount()).isEqualTo(0L);
        }
    }

    @Nested
    @DisplayName("피드 조회 정렬 테스트")
    class FeedReadSortingTests {

        private Feed newerFeed, olderFeed;
        private FeedDto newerDto, olderDto;

        @BeforeEach
        void setUp() {
            // Given
            Instant older = FIXED_NOW.minusSeconds(60);
            Instant newer = FIXED_NOW;

            newerFeed = newFeedAt(UUID.randomUUID(), "최신 피드", newer);
            olderFeed = newFeedAt(UUID.randomUUID(), "이전 피드", older);

            newerDto = newDtoFrom(newerFeed, "맑음", "없음", 5L, 1);
            olderDto = newDtoFrom(olderFeed, "맑음", "없음", 3L, 0);
        }

        @ParameterizedTest(name = "[{index}] createdAt {0}")
        @ValueSource(strings = {"DESCENDING", "ASCENDING"})
        void createdAt에_따라_정렬한다(String dir) {
            // Given
            String sortBy = "createdAt";

            List<UUID> idOrder = "DESCENDING".equalsIgnoreCase(dir)
                ? List.of(newerFeed.getId(), olderFeed.getId())
                : List.of(olderFeed.getId(), newerFeed.getId());

            CursorPageResponse<UUID> idPage = new CursorPageResponse<>(
                idOrder, null, null, false, 2L, sortBy, dir
            );

            given(esFeedRepository.searchIds(
                null, null, LIMIT, sortBy, dir, null, null, null, null
            )).willReturn(idPage);

            given(feedRepository.findAllById(idOrder))
                .willReturn(List.of(olderFeed, newerFeed));

            given(esFeedRepository.countByFilters(null, null, null, null)).willReturn(2L);
            given(feedMapper.toDto(newerFeed)).willReturn(newerDto);
            given(feedMapper.toDto(olderFeed)).willReturn(olderDto);

            // When
            CursorPageResponse<FeedDto> result =
                feedService.getFeeds(null, null, LIMIT, sortBy, dir, null, null, null, null);

            // Then
            if ("DESCENDING".equalsIgnoreCase(dir)) {
                assertThat(result.data()).containsExactly(newerDto, olderDto);
            } else {
                assertThat(result.data()).containsExactly(olderDto, newerDto);
            }
            assertThat(result.totalCount()).isEqualTo(2L);
        }

        @ParameterizedTest(name = "[{index}] likeCount {0}")
        @ValueSource(strings = {"DESCENDING", "ASCENDING"})
        void likeCount에_따라_정렬한다(String dir) {
            // Given
            String sortBy = "likeCount";

            List<UUID> idOrder = "DESCENDING".equalsIgnoreCase(dir)
                ? List.of(newerFeed.getId(), olderFeed.getId())
                : List.of(olderFeed.getId(), newerFeed.getId());

            CursorPageResponse<UUID> idPage = new CursorPageResponse<>(
                idOrder,
                null,
                null,
                false,
                2L,
                sortBy,
                dir
            );
            given(esFeedRepository.searchIds(
                null, null, LIMIT, sortBy, dir,
                null, null, null, null
            )).willReturn(idPage);

            given(feedRepository.findAllById(idOrder))
                .willReturn(List.of(olderFeed, newerFeed));

            given(esFeedRepository.countByFilters(null, null, null, null))
                .willReturn(2L);
            given(feedMapper.toDto(newerFeed)).willReturn(newerDto);
            given(feedMapper.toDto(olderFeed)).willReturn(olderDto);

            // When
            CursorPageResponse<FeedDto> result =
                feedService.getFeeds(null, null, LIMIT, sortBy, dir, null, null, null, null);

            // Then
            if ("DESCENDING".equalsIgnoreCase(dir)) {
                assertThat(result.data()).containsExactly(newerDto, olderDto);
            } else {
                assertThat(result.data()).containsExactly(olderDto, newerDto);
            }
            assertThat(result.totalCount()).isEqualTo(2L);
        }
    }
}
