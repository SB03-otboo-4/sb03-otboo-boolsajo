package com.sprint.otboo.feed.service;

import com.sprint.otboo.clothing.entity.Clothes;
import com.sprint.otboo.clothing.repository.ClothesRepository;
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
import jakarta.persistence.EntityNotFoundException;
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
            .orElseThrow(
                () -> new EntityNotFoundException("User not found: " + request.authorId()));
        Weather weather = weatherRepository.findById(request.weatherId())
            .orElseThrow(
                () -> new EntityNotFoundException("Weather not found: " + request.weatherId()));

        Feed feed = Feed.builder()
            .author(author)
            .weather(weather)
            .content(request.content().trim())
            .likeCount(0L)
            .commentCount(0L)
            .build();

        Feed saved = feedRepository.save(feed);

        List<UUID> clothesIds = Optional.ofNullable(request.clothesIds()).orElseGet(List::of);
        if (!clothesIds.isEmpty()) {
            List<UUID> distinctIds = clothesIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

            List<Clothes> clothesList = clothesRepository.findAllByIdInAndUser_Id(distinctIds,
                author.getId());

            if (clothesList.size() != distinctIds.size()) {
                Set<UUID> foundIds = clothesList.stream().map(Clothes::getId)
                    .collect(Collectors.toSet());
                List<UUID> missing = distinctIds.stream().filter(id -> !foundIds.contains(id))
                    .toList();
                throw new EntityNotFoundException(
                    "Invalid clothes ids for user " + author.getId() + ": " + missing);
            }

            Instant now = Instant.now();
            List<FeedClothes> links = new ArrayList<>(clothesList.size());
            for (Clothes c : clothesList) {
                if (!feedClothesRepository.existsByFeed_IdAndClothes_Id(saved.getId(), c.getId())) {
                    links.add(FeedClothes.builder()
                        .feed(saved)
                        .clothes(c)
                        .createdAt(now)
                        .build());
                }
            }
            if (!links.isEmpty()) {
                feedClothesRepository.saveAll(links);
            }
        }

        return feedMapper.toDto(saved);
    }
}
