package com.sprint.otboo.feedsearch.repository;

import com.sprint.otboo.feedsearch.dto.FeedDoc;
import java.util.UUID;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface FeedSearchRepository
    extends ElasticsearchRepository<FeedDoc, UUID>, FeedSearchRepositoryCustom {

}
