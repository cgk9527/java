package com.ecommerce.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.ecommerce.common.PageResult;
import com.ecommerce.es.ProductDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 商品搜索服务(Elasticsearch)
 */
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public PageResult<ProductDocument> search(String keyword, Long categoryId, int page, int size, String sort) {
        int pageIndex = Math.max(page - 1, 0);
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;
        Long cid = categoryId;

        Query esQuery = Query.of(q -> q.bool(b -> {
            b.must(m -> m.term(t -> t.field("status").value(1)));
            if (kw != null) {
                // 通配检索 nameKeyword 字段, 无需分词插件即可匹配中文子串
                b.must(m -> m.wildcard(w -> w.field("nameKeyword").value("*" + kw + "*")));
            }
            if (cid != null) {
                b.must(m -> m.term(t -> t.field("categoryId").value(cid)));
            }
            return b;
        }));

        NativeQuery query = NativeQuery.builder()
                .withQuery(esQuery)
                .withSort(buildSort(sort))
                .withPageable(PageRequest.of(pageIndex, size))
                .build();

        SearchHits<ProductDocument> hits = elasticsearchOperations.search(query, ProductDocument.class);
        List<ProductDocument> records = hits.stream().map(SearchHit::getContent).toList();
        return PageResult.of(hits.getTotalHits(), page, size, records);
    }

    public List<ProductDocument> hot(int limit) {
        Query esQuery = Query.of(q -> q.term(t -> t.field("status").value(1)));
        NativeQuery query = NativeQuery.builder()
                .withQuery(esQuery)
                .withSort(Sort.by(Sort.Order.desc("sales")))
                .withPageable(PageRequest.of(0, limit))
                .build();
        return elasticsearchOperations.search(query, ProductDocument.class)
                .stream().map(SearchHit::getContent).toList();
    }

    private Sort buildSort(String sort) {
        if (StringUtils.hasText(sort)) {
            return switch (sort) {
                case "price_asc" -> Sort.by(Sort.Order.asc("price"));
                case "price_desc" -> Sort.by(Sort.Order.desc("price"));
                default -> Sort.by(Sort.Order.desc("sales"));
            };
        }
        return Sort.by(Sort.Order.desc("sales"));
    }
}