package com.sprint.otboo.feedsearch.bootstrap;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * [EsIndexBootstrapper] - 앱 기동 시 ES 인덱스가 없으면 생성한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EsIndexBootstrapper {

    private final ElasticsearchClient es;

    // 인덱스 이름과 매핑/세팅 JSON 경로는 여기서 관리
    private static final String INDEX_NAME = "feeds_v1-000001";
    private static final String INDEX_JSON = "es/feeds_index.json";

    /**
     * [EsIndexBootstrapper] 인덱스 보장(없으면 생성)
     * - idempotent: 여러 번 호출해도 안전
     * - 실패 시 IllegalStateException으로 감싸 상위로 전파(프리플라이트 단계에서 바로 확인)
     */
    public void ensure() {
        try {
            boolean exists = es.indices()
                .exists(ExistsRequest.of(b -> b.index(INDEX_NAME)))
                .value();

            if (exists) {
                log.debug("[EsIndexBootstrapper] 인덱스 이미 존재: {}", INDEX_NAME);
                return;
            }

            log.info("[EsIndexBootstrapper] 인덱스 생성 시작: {}", INDEX_NAME);
            createIndexFromJson(INDEX_NAME, INDEX_JSON);
            log.info("[EsIndexBootstrapper] 인덱스 생성 완료: {}", INDEX_NAME);

        } catch (IOException e) {
            throw new IllegalStateException("[EsIndexBootstrapper] 인덱스 보장 실패: " + INDEX_NAME, e);
        }
    }

    /**
     * [EsIndexBootstrapper] 클래스패스 JSON으로 인덱스 생성
     */
    private void createIndexFromJson(String indexName, String classpathJson) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpathJson);
        try (Reader reader = new InputStreamReader(resource.getInputStream(),
            StandardCharsets.UTF_8)) {
            es.indices().create(c -> c.index(indexName).withJson(reader));
        }
    }
}
