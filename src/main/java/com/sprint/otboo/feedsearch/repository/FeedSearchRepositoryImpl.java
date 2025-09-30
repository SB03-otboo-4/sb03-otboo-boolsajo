package com.sprint.otboo.feedsearch.repository;

import com.sprint.otboo.common.dto.CursorPageResponse;
import com.sprint.otboo.feedsearch.dto.FeedDoc;
import com.sprint.otboo.weather.entity.PrecipitationType;
import com.sprint.otboo.weather.entity.SkyStatus;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class FeedSearchRepositoryImpl implements FeedSearchRepositoryCustom {

    private static final String F_CONTENT = "content";
    private static final String F_CONTENT_KW = "content.keyword";
    private static final String F_CONTENT_NGRAM = "content.ngram";
    private static final String F_CONTENT_NOSPACE = "content.nospace";
    private static final String F_CONTENT_SHINGLE = "content.shingle";
    private static final String F_AUTHOR_NAME = "author.name";

    private final ElasticsearchOperations es;
    private static final IndexCoordinates INDEX = IndexCoordinates.of("feeds");

    @Override
    public CursorPageResponse<UUID> searchIds(
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
        final boolean desc = !"ASCENDING".equalsIgnoreCase(sortDirection);
        final String primary = "likeCount".equals(sortBy) ? "likeCount" : "createdAt";
        final int size = Math.min(Math.max(limit, 1), 100) + 1;

        Query bool = buildBool(keywordLike, skyStatus, precipitationType, authorId);

        List<SortOptions> sorts = buildSorts(primary, desc);

        List<Object> searchAfter = buildSearchAfter(primary, cursor, idAfter);

        NativeQueryBuilder qb = new NativeQueryBuilder()
            .withQuery(bool)
            .withSort(sorts)
            .withPageable(PageRequest.of(0, size));
        if (searchAfter != null) {
            qb.withSearchAfter(searchAfter);
        }

        SearchHits<FeedDoc> hits = es.search(qb.build(), FeedDoc.class, INDEX);
        List<SearchHit<FeedDoc>> raw = hits.getSearchHits();

        boolean hasNext = raw.size() > limit;
        List<SearchHit<FeedDoc>> page = hasNext ? raw.subList(0, limit) : raw;

        List<UUID> ids = page.stream()
            .map(h -> UUID.fromString(String.valueOf(Objects.requireNonNull(h.getContent().id()))))
            .toList();

        String nextCursor = null;
        String nextIdAfter = null;

        if (hasNext && !page.isEmpty()) {
            NextCursor nc = buildNextCursor(page);
            nextCursor = nc.cursor();
            nextIdAfter = nc.idAfter();
        }

        return new CursorPageResponse<>(
            ids,
            nextCursor,
            nextIdAfter,
            hasNext,
            0L,
            primary,
            desc ? "DESCENDING" : "ASCENDING"
        );
    }

    private List<SortOptions> buildSorts(String primary, boolean desc) {
        SortOrder order = desc ? SortOrder.Desc : SortOrder.Asc;
        return List.of(
            SortOptions.of(s -> s.field(f -> f.field(primary).order(order))),
            SortOptions.of(s -> s.field(f -> f.field("id").order(order)))
        );
    }

    private List<Object> buildSearchAfter(String primary, String cursor, UUID idAfter) {
        if (cursor == null || idAfter == null) {
            return null;
        }
        Object pv = parseCursorValue(primary, cursor);
        return List.of(pv, idAfter.toString());
    }

    private NextCursor buildNextCursor(List<SearchHit<FeedDoc>> page) {
        List<Object> sv = page.get(page.size() - 1).getSortValues();
        Object s0 = sv.get(0);
        String cursor =
            (s0 instanceof Number) ? String.valueOf(((Number) s0).longValue()) : s0.toString();
        String idAfter = sv.get(1).toString();
        return new NextCursor(cursor, idAfter);
    }

    private record NextCursor(String cursor, String idAfter) {

    }

    private Object parseCursorValue(String primary, String cursor) {
        if (cursor == null) {
            return null;
        }
        if ("likeCount".equals(primary)) {
            return Long.parseLong(cursor);
        }
        boolean numeric = cursor.chars().allMatch(Character::isDigit);
        return numeric ? Long.parseLong(cursor) : cursor;
    }

    private static String noSpace(String s) {
        return (s == null) ? null : s.replaceAll("\\s+", "");
    }

    @Override
    @Transactional(readOnly = true)
    public long countByFilters(
        String keywordLike,
        SkyStatus skyStatus,
        PrecipitationType precipitationType,
        UUID authorId
    ) {
        Query bool = buildBool(keywordLike, skyStatus, precipitationType, authorId);

        NativeQuery query = new NativeQueryBuilder()
            .withQuery(bool)
            .withMaxResults(0)
            .build();

        long cnt = es.count(query, INDEX);

        if (log.isDebugEnabled()) {
            log.debug(
                "[EsFeedRepository] countByFilters: count={}, keyword={}, skyStatus={}, precipitationType={}, authorId={}",
                cnt, keywordLike, skyStatus, precipitationType, authorId
            );
        }
        return cnt;
    }

    private Query buildBool(
        String keywordLike,
        SkyStatus skyStatus,
        PrecipitationType precipitationType,
        UUID authorId
    ) {
        final List<Query> must = new ArrayList<>();
        final List<Query> should = new ArrayList<>();
        final List<Query> filters = new ArrayList<>();

        final String q = (keywordLike == null) ? "" : keywordLike.trim();
        final boolean hasQ = !q.isBlank();
        final String qNoSpace = hasQ ? noSpace(q) : null;

        if (!hasQ) {
            // 검색어 없으면 전체 조회
            must.add(MatchAllQuery.of(m -> m)._toQuery());
        } else {
            // 1) 정확 일치 (가중치 최상)
            should.add(new Query.Builder().term(t -> t
                .field(F_CONTENT_KW)
                .value(q)
                .boost(8.0f)
            ).build());

            // 2) 형태소 기본 매치 (AND, best_fields)
            should.add(new Query.Builder().multiMatch(mm -> mm
                .query(q)
                .fields(F_CONTENT + "^3", F_AUTHOR_NAME + "^1.2")
                .operator(Operator.And)
                .type(TextQueryType.BestFields)
                .boost(3.0f)
            ).build());

            // 3) 문구(shingle) — 띄어쓰기 변형/연속어 보조
            should.add(new Query.Builder().matchPhrase(mp -> mp
                .field(F_CONTENT_SHINGLE)
                .query(q)
                .slop(2)
                .boost(2.5f)
            ).build());

            // 4) 오타 허용(fuzzy) — 한국어 과확장 방지용 낮은 가중치
            should.add(new Query.Builder().multiMatch(mm -> mm
                .query(q)
                .fields(F_CONTENT, F_AUTHOR_NAME)
                .fuzziness("AUTO")
                .prefixLength(1)
                .maxExpansions(30)
                .boost(1.5f)
            ).build());

            // 5) 부분일치(edge n-gram)
            should.add(new Query.Builder().multiMatch(mm -> mm
                .query(q)
                .fields(F_CONTENT_NGRAM + "^1.3")
                .boost(1.3f)
            ).build());

            // 6) 띄어쓰기 오류(no-space) 보정
            if (qNoSpace != null && !qNoSpace.isBlank()) {
                should.add(new Query.Builder().multiMatch(mm -> mm
                    .query(qNoSpace)
                    .fields(F_CONTENT_NOSPACE + "^1.6")
                    .boost(1.6f)
                ).build());
            }
        }

        if (authorId != null) {
            filters.add(new Query.Builder()
                .term(t -> t.field("author.userId").value(authorId.toString()))
                .build());
        }
        if (skyStatus != null) {
            filters.add(new Query.Builder()
                .term(t -> t.field("weather.skyStatus").value(skyStatus.name()))
                .build());
        }
        if (precipitationType != null) {
            filters.add(new Query.Builder()
                .term(t -> t.field("weather.precipitation.type").value(precipitationType.name()))
                .build());
        }
        return new Query.Builder().bool(b -> b
            .must(must)
            .should(should)
            .filter(filters)
            .minimumShouldMatch(hasQ ? "1" : null)
        ).build();
    }
}
