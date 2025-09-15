package com.sprint.otboo.feed.service;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.repository.ClothesRepository;
import com.sprint.otboo.common.exception.clothing.UserClothesNotFoundException;
import com.sprint.otboo.common.exception.user.UserNotFoundException;
import com.sprint.otboo.common.exception.weather.WeatherNotFoundException;
import com.sprint.otboo.feed.dto.data.FeedDto;
import com.sprint.otboo.feed.dto.request.FeedCreateRequest;
import com.sprint.otboo.feed.entity.Feed;
import com.sprint.otboo.feed.entity.FeedClothes;
import com.sprint.otboo.feed.mapper.FeedMapper;
import com.sprint.otboo.feed.repository.FeedClothesRepository;
import com.sprint.otboo.feed.repository.FeedRepository;
import com.sprint.otboo.user.entity.User;
import com.sprint.otboo.user.repository.UserRepository;
import com.sprint.otboo.weather.entity.Weather;
import com.sprint.otboo.weather.repository.WeatherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedServiceImpl implements FeedService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final WeatherRepository weatherRepository;
    private final ClothesRepository clothesRepository;
    private final FeedClothesRepository feedClothesRepository;
    private final FeedMapper feedMapper;

    @Override
    @Transactional
    public FeedDto create(FeedCreateRequest request) {
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

        Set<UUID> clothesIds = distinctIdSet(request.clothesIds());
        if (!clothesIds.isEmpty()) {
            List<Clothes> clothesList =
                clothesRepository.findAllByIdInAndUser_Id(new ArrayList<>(clothesIds),
                    author.getId());
            validateAllFound(author.getId(), clothesIds, clothesList);
            linkFeedWithClothes(saved, clothesList);
        }

        return feedMapper.toDto(saved);
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

            throw UserClothesNotFoundException.withIds(userId, missing);
        }
    }

    private void linkFeedWithClothes(Feed saved, List<Clothes> clothesList) {
        Instant now = Instant.now();
        List<FeedClothes> links = clothesList.stream()
            .map(c -> FeedClothes.builder()
                .feed(saved)
                .clothes(c)
                .createdAt(now)
                .build())
            .collect(Collectors.toList());

        feedClothesRepository.saveAll(links);
        saved.getFeedClothes().addAll(links);
    }
}
