package com.sprint.otboo.feed.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.otboo.feed.dto.request.CommentCreateRequest;
import com.sprint.otboo.feed.entity.Comment;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.repository.CommentRepository;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.fixture.CommentFixture;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;
    @Autowired
    WeatherLocationRepository weatherLocationRepository;
    @Autowired
    WeatherRepository weatherRepository;
    @Autowired
    FeedRepository feedRepository;
    @Autowired
    CommentRepository commentRepository;

    private UUID feedId;
    private UUID authorId;
    private User author;
    private Feed feed;

    @BeforeEach
    void setUp() {
        // given
        author = userRepository.save(UserFixture.createUserWithDefault());
        authorId = author.getId();

        WeatherLocation location = weatherLocationRepository.save(
            WeatherLocationFixture.createLocationWithDefault()
        );
        Weather weather = weatherRepository.save(
            WeatherFixture.createWeatherWithDefault(location)
        );

        feed = feedRepository.save(
            Feed.builder()
                .author(author)
                .weather(weather)
                .content("피드 본문")
                .createdAt(Instant.now())
                .build()
        );
        feedId = feed.getId();
    }

    @Nested
    @DisplayName("댓글 등록 통합 테스트")
    class CommentCreateIntegrationTests {

        @Test
        void 성공하면_201과_DTO를_반환하고_DB에_저장한다() throws Exception {
            // given
            CommentCreateRequest request = new CommentCreateRequest(feedId, authorId, "첫 댓글");

            // when
            mockMvc.perform(
                    post("/api/feeds/{feedId}/comments", feedId)
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                // then
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").value("첫 댓글"))
                .andExpect(jsonPath("$.feedId").value(feedId.toString()))
                .andExpect(jsonPath("$.author.name").value("홍길동"));

            List<Comment> saved = commentRepository.findAllByFeedIdOrderByCreatedAtDesc(feedId);
            assertThat(saved).isNotEmpty();
            assertThat(saved.get(0).getContent()).isEqualTo("첫 댓글");
            assertThat(saved.get(0).getAuthor()).isEqualTo(author);
        }

        @Test
        void 내용이_공백이면_400을_반환한다() throws Exception {
            // given
            CommentCreateRequest request = new CommentCreateRequest(feedId, authorId, "   ");

            // when
            mockMvc.perform(
                    post("/api/feeds/{feedId}/comments", feedId)
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                // then
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        void 존재하지_않는_feedId면_404를_반환한다() throws Exception {
            // given
            UUID notExistsFeedId = UUID.randomUUID();
            CommentCreateRequest request = new CommentCreateRequest(notExistsFeedId, authorId,
                "첫 댓글");

            // when
            mockMvc.perform(
                    post("/api/feeds/{feedId}/comments", notExistsFeedId)
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                // then
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        void 존재하지_않는_작성자면_404를_반환한다() throws Exception {
            // given
            UUID missingAuthorId = UUID.randomUUID();
            CommentCreateRequest request = new CommentCreateRequest(feedId, missingAuthorId,
                "첫 댓글");

            // when
            mockMvc.perform(
                    post("/api/feeds/{feedId}/comments", feedId)
                        .with(csrf())
                        .with(user("tester").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request))
                )
                // then
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("댓글 조회 통합 테스트")
    class CommentReadIntegrationTests {

        @Test
        void 정상적으로_조회하면_200과_페이지_DTO를_반환한다() throws Exception {
            // given
            Comment oldComment = CommentFixture.create(
                author,
                feed,
                "둘째 댓글",
                Instant.now().minusSeconds(10)
            );
            Comment recentComment = CommentFixture.create(
                author,
                feed,
                "첫 댓글",
                Instant.now()
            );
            commentRepository.saveAll(List.of(oldComment, recentComment));

            // when
            mockMvc.perform(
                    get("/api/feeds/{feedId}/comments", feedId)
                        .with(user("tester").roles("USER"))
                        .param("limit", "2")
                        .accept(MediaType.APPLICATION_JSON)
                )
                // then
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].content").value("첫 댓글"))
                .andExpect(jsonPath("$.data[0].feedId").value(feedId.toString()))
                .andExpect(jsonPath("$.data[0].author.name").value("홍길동"))
                .andExpect(jsonPath("$.hasNext").isBoolean());
        }

        @Test
        void limit이_없으면_400을_반환한다() throws Exception {

            // when
            mockMvc.perform(
                    get("/api/feeds/{feedId}/comments", feedId)
                        .with(user("tester").roles("USER"))
                        .accept(MediaType.APPLICATION_JSON)
                )
                // then
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        void 존재하지_않는_feedId면_404를_반환한다() throws Exception {
            // given
            UUID missingFeedId = UUID.randomUUID();

            // when
            mockMvc.perform(
                    get("/api/feeds/{feedId}/comments", missingFeedId)
                        .with(user("tester").roles("USER"))
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON)
                )
                // then
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }
}
