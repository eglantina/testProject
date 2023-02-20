package test.repository.search;

import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;

import java.util.stream.Stream;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import test.domain.Conference;

/**
 * Spring Data Elasticsearch repository for the {@link Conference} entity.
 */
public interface ConferenceSearchRepository extends ElasticsearchRepository<Conference, Long>, ConferenceSearchRepositoryInternal {}

interface ConferenceSearchRepositoryInternal {
    Stream<Conference> search(String query);
}

class ConferenceSearchRepositoryInternalImpl implements ConferenceSearchRepositoryInternal {

    private final ElasticsearchRestTemplate elasticsearchTemplate;

    ConferenceSearchRepositoryInternalImpl(ElasticsearchRestTemplate elasticsearchTemplate) {
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    @Override
    public Stream<Conference> search(String query) {
        NativeSearchQuery nativeSearchQuery = new NativeSearchQuery(queryStringQuery(query));
        return elasticsearchTemplate.search(nativeSearchQuery, Conference.class).map(SearchHit::getContent).stream();
    }
}
