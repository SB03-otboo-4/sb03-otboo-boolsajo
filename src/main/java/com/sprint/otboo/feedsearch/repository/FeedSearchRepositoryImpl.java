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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 피드 검색용 커스텀 리포지토리 구현체
 *
 * <p>Elasticsearch에 저장된 피드 문서({@link FeedDoc})를 대상으로
 * - 키워드(형태소/문구/부분/오타/띄어쓰기 보정) 가중치 검색, - 작성자/날씨 조건 필터, - 정렬 및 search_after 기반 커서 페이지네이션을 수행한다.</p>
 *
 * <h2>정렬/페이지네이션</h2>
 * <ul>
 *   <li>정렬 기본 필드: <code>createdAt</code> (또는 요청 시 <code>likeCount</code>)</li>
 *   <li>타이브레이커: <code>id</code> (UUID 문자열)</li>
 *   <li>커서: <code>search_after</code> 를 사용하며, [primarySortValue, id] 형태</li>
 * </ul>
 *
 * <h2>검색 가중치 구성</h2>
 * <ol>
 *   <li>정확일치: <code>content.kw</code> (boost 8.0)</li>
 *   <li>형태소 기본 매치: <code>content^3, author.name^1.2</code> (AND, best_fields, boost 3.0)</li>
 *   <li>문구(shingle): <code>content.shingle</code> (slop 2, boost 2.5)</li>
 *   <li>오타 허용(fuzzy): <code>content, author.name</code> (boost 1.5)</li>
 *   <li>부분일치(edge n-gram): <code>content.ngram</code> (boost 1.3)</li>
 *   <li>띄어쓰기 보정(no-space): <code>content.nospace</code> (boost 1.6)</li>
 * </ol>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FeedSearchRepositoryImpl implements FeedSearchRepositoryCustom {

    static final class F {

        static final String CONTENT = "content";
        static final String CONTENT_KW = "content.kw";
        static final String CONTENT_NGRAM = "content.ngram";
        static final String CONTENT_NOSPACE = "content.nospace";
        static final String CONTENT_SHINGLE = "content.shingle";
        static final String AUTHOR_NAME = "author.name";
        static final String AUTHOR_ID = "author.userId";
        static final String SKY = "weather.skyStatus";
        static final String PRECIP = "weather.precipitation.type";
        static final String CREATED_AT = "createdAt";
        static final String LIKE_COUNT = "likeCount";
        static final String ID = "id";
    }

    private final ElasticsearchOperations es;

    @Value("${app.index.read-alias:feed-read}")
    private String readAlias;

    private IndexCoordinates index() {
        return IndexCoordinates.of(readAlias);
    }

    /**
     * 피드 ID 목록을 커서 페이지네이션으로 조회한다.
     *
     * @param cursor            직전 페이지의 1차 정렬 필드 값(문자열). likeCount/createdAt 값이 들어온다.
     * @param idAfter           직전 페이지의 마지막 문서 id(UUID). search_after의 2번째 키
     * @param limit             페이지 크기(최대 100)
     * @param sortBy            정렬 기준: "likeCount" 또는 "createdAt"
     * @param sortDirection     "ASCENDING" 또는 "DESCENDING" (기본: DESC)
     * @param keywordLike       검색어(가중치 검색). null/blank면 전체조회
     * @param skyStatus         하늘 상태 필터 (선택)
     * @param precipitationType 강수 유형 필터 (선택)
     * @param authorId          작성자 ID 필터 (선택)
     * @return ID 목록과 다음 커서 정보가 포함된 {@link CursorPageResponse}
     */
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
        final String primary = F.LIKE_COUNT.equals(sortBy) ? F.LIKE_COUNT : F.CREATED_AT;
        final int size = Math.min(Math.max(limit, 1), 100) + 1;

        Query bool = buildBool(keywordLike, skyStatus, precipitationType, authorId);
        List<SortOptions> sorts = buildSorts(primary, desc);
        List<Object> searchAfter = buildSearchAfter(cursor, idAfter);

        NativeQueryBuilder qb = new NativeQueryBuilder()
            .withQuery(bool)
            .withSort(sorts)
            .withPageable(PageRequest.of(0, size));

        if (searchAfter != null) {
            qb.withSearchAfter(searchAfter);
        }

        SearchHits<FeedDoc> hits = es.search(qb.build(), FeedDoc.class, index());
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

    /**
     * 정렬 옵션을 구성한다. 1차 정렬(primary) + 타이브레이커(id)
     */
    private List<SortOptions> buildSorts(String primary, boolean desc) {
        SortOrder order = desc ? SortOrder.Desc : SortOrder.Asc;
        return List.of(
            SortOptions.of(s -> s.field(f -> f.field(primary).order(order))),
            SortOptions.of(s -> s.field(f -> f.field(F.ID).order(order)))
        );
    }

    /**
     * search_after 값을 구성한다. [primarySortValue, idAfter] 형태
     */
    private List<Object> buildSearchAfter(String cursor, UUID idAfter) {
        if (cursor == null || idAfter == null) {
            return null;
        }
        Long primarySortValue = parseCursorValue(cursor);
        return List.of(primarySortValue, idAfter.toString());
    }

    /**
     * 다음 페이지 커서 구성을 위해 마지막 히트의 sort_values 파싱
     */
    private NextCursor buildNextCursor(List<SearchHit<FeedDoc>> page) {
        List<Object> sortValues = page.get(page.size() - 1).getSortValues();
        long primarySortValue = ((Number) sortValues.get(0)).longValue();
        String idAfter = sortValues.get(1).toString();
        return new NextCursor(String.valueOf(primarySortValue), idAfter);
    }

    /**
     * 커서 DTO
     */
    private record NextCursor(String cursor, String idAfter) {

    }

    /**
     * 커서 문자열을 정렬 기준 타입에 맞게 파싱 허용
     */
    private Long parseCursorValue(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cursor must be epoch_millis long: " + cursor, e);
        }
    }

    /**
     * 공백 제거 유틸
     */
    private static String noSpace(String s) {
        return (s == null) ? null : s.replaceAll("\\s+", "");
    }

    /**
     * 동일한 필터 조건으로 문서 수를 카운트한다.
     */
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

        long cnt = es.count(query, index());

        if (log.isDebugEnabled()) {
            log.debug(
                "[FeedSearchRepositoryImpl] countByFilters: count={}, keyword={}, skyStatus={}, precipitationType={}, authorId={}",
                cnt, keywordLike, skyStatus, precipitationType, authorId
            );
        }
        return cnt;
    }

    /**
     * bool 쿼리를 구성한다.
     * <ul>
     *   <li>검색어가 없으면 match_all</li>
     *   <li>검색어가 있으면 should 절에 가중치 쿼리들을 추가하고 minimum_should_match=1</li>
     *   <li>작성자/날씨/강수 필터는 filter 절로 고정</li>
     * </ul>
     */
    private Query buildBool(String keywordLike, SkyStatus sky, PrecipitationType type,
        UUID authorId) {
        final List<Query> must = new ArrayList<>();
        final List<Query> should = new ArrayList<>();
        final List<Query> filters = new ArrayList<>();

        final String searchText = (keywordLike == null) ? "" : keywordLike.trim();
        final boolean hasSearchText = !searchText.isBlank();
        final String searchTextNoSpace = hasSearchText ? noSpace(searchText) : null;

        if (!hasSearchText) {
            must.add(MatchAllQuery.of(m -> m)._toQuery());
        } else {
            addWeightedShouldQueries(should, searchText, searchTextNoSpace);
        }

        if (authorId != null) {
            filters.add(
                new Query.Builder().term(t -> t.field(F.AUTHOR_ID).value(authorId.toString()))
                    .build());
        }
        if (sky != null) {
            filters.add(new Query.Builder().term(t -> t.field(F.SKY).value(sky.name())).build());
        }
        if (type != null) {
            filters.add(
                new Query.Builder().term(t -> t.field(F.PRECIP).value(type.name())).build());
        }

        return new Query.Builder().bool(b -> b.must(must).should(should).filter(filters)
            .minimumShouldMatch(hasSearchText ? "1" : null)).build();
    }

    private void addWeightedShouldQueries(List<Query> should, String searchText,
        String searchTextNoSpace) {
        // 1) 정확 일치: content.kw (keyword)
        should.add(new Query.Builder()
            .term(t -> t.field(F.CONTENT_KW).value(searchText).boost(8.0f))
            .build());

        // 2) 형태소 기본 매치: content^3, author.name^1.2 (nori, BestFields, AND)
        should.add(new Query.Builder()
            .multiMatch(mm -> mm
                .query(searchText)
                .fields(F.CONTENT + "^3", F.AUTHOR_NAME + "^1.2")
                .operator(Operator.And)
                .type(TextQueryType.BestFields)
                .boost(3.0f))
            .build());

        // 3) 문구(shingle): content.shingle (slop 2)
        should.add(new Query.Builder()
            .matchPhrase(mp -> mp
                .field(F.CONTENT_SHINGLE)
                .query(searchText)
                .slop(2)
                .boost(2.5f))
            .build());

        // 4) 오타 허용(fuzzy): content, author.name
        should.add(new Query.Builder()
            .multiMatch(mm -> mm
                .query(searchText)
                .fields(F.CONTENT, F.AUTHOR_NAME)
                .fuzziness("AUTO")
                .prefixLength(1)
                .maxExpansions(30)
                .boost(1.5f))
            .build());

        // 5) 부분일치(n-gram): content.ngram  (ko_edge = ngram_2_20 기반)
        should.add(new Query.Builder()
            .match(m -> m
                .field(F.CONTENT_NGRAM)
                .query(searchText)
                .boost(1.3f))
            .build());

        // 6) 띄어쓰기 보정(no-space): content.nospace  (keyword+lowercase)
        if (searchTextNoSpace != null && !searchTextNoSpace.isBlank()) {
            should.add(new Query.Builder()
                .match(m -> m
                    .field(F.CONTENT_NOSPACE)
                    .query(searchTextNoSpace)
                    .boost(1.6f))
                .build());
        }
    }
}
