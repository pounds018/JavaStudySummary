package cn.pounds;

import cn.pounds.domain.Goods;
import cn.pounds.mapper.GoodsMapper;
import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/**
 * @Project: 07_elasticsearch
 * @Date: 2020/10/29 19:25
 * @author: by Martin
 * @Description: 各种查询
 */
@SpringBootTest
public class SearchTest {
    @Resource
    private RestHighLevelClient esClient;
    @Resource
    private GoodsMapper goodsMapper;

    /**
        需求:
              1.查询所有数据
              2.将查询结果封装成goods对象,封装进list
              3.分页,默认显示10条
     */
    @Test
    public void MatchAllTest() throws IOException {
        // 2. 封装查询请求
        SearchRequest searchRequest = new SearchRequest("goods");
        // 4.创建资源构建器
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        // 6. 创建查询构建器,所有的查询方式就在这里选择
        QueryBuilder queryBuilder = QueryBuilders.matchAllQuery();
        // 5.指定查询条件构建器, 并构建查询条件,如果不加分页查询的处理,只会显示10条
        sourceBuilder.query(queryBuilder).from(0).size(1000);
        // 3. 添加查询构建器
        searchRequest.source(sourceBuilder);
        // 1.查询
        SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);

        // 获取结果,hits里面还有一个hits,那个hits才是封装文档的,第一个hits 里面还封装的查询结果的一些统计数据
        // 获取查询结果总数
        System.out.println(searchResponse.getHits().getTotalHits());

        // 从第二个hits里面获取真正的数据
        SearchHit[] hits = searchResponse.getHits().getHits();
        ArrayList<Goods> goodsList = new ArrayList<>();
        for (SearchHit hit : hits) {
            // 从单个结果的source域里面那出数据,并封装成goods
            Goods goods = JSON.parseObject(hit.getSourceAsString(), Goods.class);
            // 装进list里面
            goodsList.add(goods);
        }
        // 默认是分好页的,不修改分页的话,只会返回分页数据.默认为10条
        System.out.println("封装了的数量为 ---> "+goodsList.size());
    }

    /**
     * term-query 词条不分词查询
     */
    @Test
    public void termQueryTest() throws IOException {
        // 创建searchRequest
        SearchRequest searchRequest = new SearchRequest("goods");
        // 获取queryBuilder,sourceBuilder
        QueryBuilder queryBuilder = QueryBuilders.termQuery("title","华为手机");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 构建查询条件
        SearchSourceBuilder sourceBuilder = searchSourceBuilder.query(queryBuilder);
        // 封装sourceBuilder进request
        searchRequest.source(sourceBuilder);
        // 执行查询
        SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);
        System.out.println(searchResponse.getHits().getTotalHits());
    }

    /**
     * match-query 词条分词查询
     */
    @Test
    public void matchQueryTest() throws IOException {
        // 创建 查询请求
        SearchRequest searchRequest = new SearchRequest("goods");
        // 创建 资源构建器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 创建 查询构建器,并设置为or
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("title", "华为手机").operator(Operator.OR);
        // 封装查询条件
        searchSourceBuilder.query(matchQueryBuilder);
        // 封装资源
        searchRequest.source(searchSourceBuilder);
        // 执行查询条件
        SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);
        long value = searchResponse.getHits().getTotalHits().value;
        System.out.println(value);

        SearchRequest searchRequest1 = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder1 = new SearchSourceBuilder();
        MatchQueryBuilder matchQueryBuilder1 = QueryBuilders.matchQuery("title", "华为手机").operator(Operator.AND);
        searchSourceBuilder1.query(matchQueryBuilder1);
        searchRequest1.source(searchSourceBuilder1);
        SearchResponse searchResponse1 = esClient.search(searchRequest1, RequestOptions.DEFAULT);
        long value1 = searchResponse1.getHits().getTotalHits().value;
        System.out.println(value1);
        System.out.println(value1 == value);
    }

    /**
     * 模糊查询
     */
    @Test
    public void queryTest() throws IOException {
        SearchRequest goods = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        QueryBuilder wildcardQueryBuilder = QueryBuilders.wildcardQuery("title", "华为");
        SearchSourceBuilder query = searchSourceBuilder.query(wildcardQueryBuilder);
        SearchRequest source = goods.source(query);
        SearchResponse search = esClient.search(source, RequestOptions.DEFAULT);
        System.out.println(search.getHits().getTotalHits().value);
    }

    /**
     * 范围查询
     */
    @Test
    public void rangeQueryTest() throws IOException {
        SearchRequest goods = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").gte(1000).lte(2000);
        //SearchSourceBuilder query = searchSourceBuilder.query(rangeQueryBuilder);
        //SearchRequest source = goods.source(query);
        searchSourceBuilder.query(rangeQueryBuilder).sort("price", SortOrder.DESC);
        goods.source(searchSourceBuilder);
        SearchResponse search = esClient.search(goods, RequestOptions.DEFAULT);
        System.out.println(search.getHits().getTotalHits().value);
    }
    /**
     * 布尔查询
     * 需求:
     *  1. 查询品牌为 华为
     *  2. 查询标题包含: 手机
     *  3. 查询价格在: 2000-3000
     */
    @Test
    public void boolQueryTest() throws IOException {
        SearchRequest goods = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 构建查询条件
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("brandName", "华为");
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("title", "手机");
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("price").lte(3000).gte(2000);
        BoolQueryBuilder filter = QueryBuilders.boolQuery().must(termQueryBuilder).filter(matchQueryBuilder).filter(rangeQueryBuilder);
        searchSourceBuilder.query(filter);
        goods.source(searchSourceBuilder);
        SearchResponse search = esClient.search(goods, RequestOptions.DEFAULT);
        System.out.println(search.getHits().getTotalHits().value);
    }

    /**
     * 桶聚合查询:
     *
     * @throws IOException
     */
    @Test
    public void bucketQueryTest() throws IOException {
        SearchRequest goods = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 构建查询条件
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("title", "手机");
        AggregationBuilder agg = AggregationBuilders.terms("good_brands").field("brandName").size(10);
        searchSourceBuilder.query(matchQueryBuilder).aggregation(agg);
        goods.source(searchSourceBuilder);
        SearchResponse search = esClient.search(goods, RequestOptions.DEFAULT);
        SearchHit[] hits = search.getHits().getHits();
        int i =0;
//        for (SearchHit hit : hits) {
//
//            Goods goods1 = JSON.parseObject(hit.getSourceAsString(), Goods.class);
//            System.out.println(i++ +"--->"+goods1);
//        }

        // 获取聚合结果:
        Aggregations aggregations = search.getAggregations();
        Map<String, Aggregation> stringAggregationMap = aggregations.asMap();
        Terms good_brands = (Terms) stringAggregationMap.get("good_brands");
        for (Terms.Bucket bucket : good_brands.getBuckets()) {
            Object key = bucket.getKey();
            System.out.println(key);
        }
    }


    /**
     * 高亮查询
     *
     * @throws IOException
     */
    @Test
    public void highlightQueryTest() throws IOException {
        SearchRequest goods = new SearchRequest("goods");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 构建查询条件
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("title", "手机");
        // 设置高亮
        searchSourceBuilder.query(matchQueryBuilder);
        HighlightBuilder highlightBuilder = new HighlightBuilder().preTags("<font color=#00ff>").field("title").postTags("</font>");
        searchSourceBuilder.highlighter(highlightBuilder);

        goods.source(searchSourceBuilder);
        SearchResponse search = esClient.search(goods, RequestOptions.DEFAULT);
        SearchHit[] hits = search.getHits().getHits();
        for (SearchHit hit : hits) {
            Goods goods1 = JSON.parseObject(hit.getSourceAsString(), Goods.class);
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            Text[] fragments = highlightField.getFragments();
            goods1.setTitle(fragments[0].toString());
            System.out.println(goods1);
        }
    }
}
