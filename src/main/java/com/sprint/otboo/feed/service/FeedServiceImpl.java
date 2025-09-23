package com.sprint.otboo.feed.service;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.common.exception.ErrorCode;
import com.sprint.otboo.common.exception.clothing.UserClothesNotFoundException;
import com.sprint.otboo.common.exception.feed.FeedNotFoundException;
import com.sprint.otboo.common.exception.paging.InvalidPagingParamException;
import com.sprint.otboo.common.exception.feed.FeedAccessDeniedException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.common.exception.weather.WeatherNotFoundException;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.dto.request.FeedUpdateRequest;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feed.repository.FeedLikeRepository;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.repository.WeatherRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final WeatherRepository weatherRepository;
    private final ClothesRepository clothesRepository;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedMapper feedMapper;

    private static final int MAX_LIMIT = 15;
    private static final Set<String> ALLOWED_SORT_BY = Set.of("createdAt", "likeCount");
    private static final Set<String> ALLOWED_SORT_DIR = Set.of("ASCENDING", "DESCENDING");

    @Override
    @Transactional
    public FeedDto create(FeedCreateRequest request) {
        log.debug("[FeedServiceImpl] 피드 생성 요청: authorId={}, weatherId={}",
            request.authorId(), request.weatherId());
        User author = userRepository.findById(request.authorId())
            .orElseThrow(() -> UserNotFoundException.withId(request.authorId()));
        Weather weather = weatherRepository.findById(request.weatherId())
            .orElseThrow(() -> WeatherNotFoundException.withId(request.weatherId()));

        Feed feed = Feed.builder()
            .author(author)
            .weather(weather)
            .content(request.content())
            .likeCount(0L)
            .commentCount(0L)
            .build();

        Feed saved = feedRepository.save(feed);
        log.info("[FeedServiceImpl] 피드 저장 완료: feedId={}", saved.getId());

        Set<UUID> clothesIds = distinctIdSet(request.clothesIds());
        if (!clothesIds.isEmpty()) {
            log.debug("[FeedServiceImpl] 의상 연결 시작: clothesIds={}", clothesIds);
            List<Clothes> clothesList =
                clothesRepository.findAllByIdInAndUser_Id(new ArrayList<>(clothesIds),
                    author.getId());
            validateAllFound(author.getId(), clothesIds, clothesList);
            linkFeedWithClothes(saved, clothesList);

            log.info("[FeedServiceImpl] 피드-의상 연결 완료: feedId={}, clothesCount={}",
                saved.getId(), clothesList.size());
        }

        return feedMapper.toDto(saved);
    }

    @Override
    @Transactional
    public FeedDto update(UUID authorId, UUID feedId, FeedUpdateRequest request) {
        log.info("[FeedServiceImpl] 피드 수정 시작: authorId={}, feedId={}, newContent={}", authorId,
            feedId, request.content());

        userRepository.findById(authorId)
            .orElseThrow(() -> UserNotFoundException.withId(authorId));

        Feed feed = feedRepository.findById(feedId)
            .orElseThrow(() -> FeedNotFoundException.withId(feedId));

        if (!feed.getAuthor().getId().equals(authorId)) {
            throw FeedAccessDeniedException.withAuthorIdAndFeedId(authorId, feedId);
        }

        String newContent = request.content();
        feed.updateContent(newContent);

        Feed saved = feedRepository.save(feed);
        log.info("[FeedServiceImpl] 피드 수정 완료: newContent={}", saved.getContent());

        return feedMapper.toDto(saved);
    }

    public CursorPageResponse<FeedDto> getFeeds(
        String cursor,
        UUID idAfter,
        int limit,
        String sortBy,
        String sortDirection,
        String keywordLike,
        SkyStatus skyStatus,
        PrecipitationType precipitationType,
        UUID authorId
    ) {

        log.info(
            "[FeedService] getFeeds in: cursor={}, idAfter={}, limit={}, sortBy={}, sortDirection={}, keyword={}, skyStatus={}, precipitationType={}, authorId={}",
            cursor, idAfter, limit, sortBy, sortDirection,
            keywordLike,
            skyStatus, precipitationType, authorId
        );
        validatePaging(limit, sortBy, sortDirection);

        List<Feed> rows = feedRepository.searchByKeyword(
            cursor,
            idAfter,
            limit,
            sortBy,
            sortDirection,
            keywordLike,
            skyStatus,
            precipitationType,
            authorId
        );
        boolean hasNext = rows.size() > limit;
        if (hasNext) {
            rows = rows.subList(0, limit);
        }

        List<FeedDto> feedDtos = rows.stream().map(feedMapper::toDto).toList();

        PageCursor pageCursor = buildNextCursor(rows, hasNext, sortBy);
        log.info("[FeedService] nextCursor built: cursor={}, idAfter={}",
            pageCursor.cursor(), pageCursor.idAfter()
        );

        long total = feedRepository.countByFilters(keywordLike, skyStatus, precipitationType,
            authorId);

        log.debug("[FeedService] total count: {}", total);

        return new CursorPageResponse<>(
            feedDtos,
            pageCursor.cursor(),
            pageCursor.idAfter(),
            hasNext,
            total,
            sortBy,
            sortDirection
        );
    }

    private void validatePaging(int limit, String sortBy, String sortDirection) {
        if (limit <= 0 || limit > MAX_LIMIT) {
            throw new InvalidPagingParamException(ErrorCode.INVALID_PAGING_LIMIT);
        }
        if (sortBy == null || !ALLOWED_SORT_BY.contains(sortBy)) {
            throw new InvalidPagingParamException(ErrorCode.INVALID_SORT_BY);
        }
        if (sortDirection == null || !ALLOWED_SORT_DIR.contains(sortDirection)) {
            throw new InvalidPagingParamException(ErrorCode.INVALID_SORT_DIRECTION);
        }
    }

    private Set<UUID> distinctIdSet(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptySet();
        }
        return ids.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateAllFound(UUID userId, Set<UUID> requestedIds, List<Clothes> found) {
        if (found.size() != requestedIds.size()) {
            Set<UUID> foundIds = found.stream().map(Clothes::getId).collect(Collectors.toSet());
            List<UUID> missing = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

            log.error("[FeedServiceImpl] 사용자의 의상 조회 실패: userId={}, missing={}",
                userId, missing);
            throw UserClothesNotFoundException.withIds(userId, missing);
        }
    }

    private void linkFeedWithClothes(Feed saved, List<Clothes> clothesList) {
        for (Clothes c : clothesList) {
            saved.addClothes(c);
            log.debug("[FeedServiceImpl] FeedClothes 엔티티 추가 완료: feedId={}", saved.getId());
        }
    }

    private PageCursor buildNextCursor(List<Feed> rows, boolean hasNext, String sortBy) {
        if (!hasNext || rows.isEmpty()) {
            return PageCursor.empty();
        }

        Feed last = rows.get(rows.size() - 1);

        String key;
        if ("likeCount".equalsIgnoreCase(sortBy)) {
            Long lc = last.getLikeCount();
            key = String.valueOf(lc);
        } else {
            Instant at = last.getCreatedAt();
            key = (at == null ? "0" : at.toString());
        }

        String nextCursor = key;
        String nextIdAfter = last.getId().toString();

        log.info("[FeedService] paging lastId={}, nextCursor={}", last.getId(), nextCursor);

        return new PageCursor(nextCursor, nextIdAfter);
    }

    private record PageCursor(String cursor, String idAfter) {

        static PageCursor empty() {
            return new PageCursor(null, null);
        }
    }
}
