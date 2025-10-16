package com.sprint.otboo.feedsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsConfig {

    /**
     * Actuator가 사용 가능한 Low-level RestClient 빈
     */
    @Bean
    public RestClient elasticsearchLowLevelClient(
        @Value("${es.host:http://localhost:9200}") String host) {
        return RestClient.builder(HttpHost.create(host)).build();
    }

    /**
     * Java Time 모듈 등록된 ObjectMapper (Instant/LocalDateTime 직렬화 지원)
     */
    @Bean
    public ObjectMapper esObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new JavaTimeModule());
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-8601
        return om;
    }

    /**
     * Elasticsearch Java API Client
     */
    @Bean
    public ElasticsearchClient elasticsearchClient(
        RestClient elasticsearchLowLevelClient,
        ObjectMapper esObjectMapper) {
        RestClientTransport transport =
            new RestClientTransport(elasticsearchLowLevelClient,
                new JacksonJsonpMapper(esObjectMapper));
        return new ElasticsearchClient(transport);
    }
}
