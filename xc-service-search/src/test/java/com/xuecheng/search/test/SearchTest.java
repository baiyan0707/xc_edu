package com.xuecheng.search.test;


import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;


//es客户端java Api测试
@SpringBootTest
@RunWith(SpringRunner.class)
public class SearchTest {

    @Autowired
    //高版本Es客户端
    RestHighLevelClient highLevelClient;

    @Autowired
    //低版本Es客户端
    RestClient restClient;

    @Test
    //查询所有
    public void testSearchAll()throws IOException{
        //搜索请求对象
        SearchRequest searchRequest = new SearchRequest("xc_course");
        //类型
        searchRequest.types("doc");
        //搜索源构建对象
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //查询所有
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        //设置需要显示的键,后面的为不用显示的
        sourceBuilder.fetchSource(new String[]{"name","studymodel","price","timestamp"},new String[]{});
        //向所有请求设置搜索源
        searchRequest.source(sourceBuilder);
        //向Es发起http请求
        SearchResponse searchResponse = highLevelClient.search(searchRequest);
        //获取到搜索结果
        SearchHits hits = searchResponse.getHits();
        //获取到个数
        long totalHits = hits.getTotalHits();
        //获取到匹配度最高的
        SearchHit[] hitsHits = hits.getHits();
        //输出
        for (SearchHit hitsHit : hitsHits) {
            Map<String, Object> sourceAsMap = hitsHit.getSourceAsMap();
            //该输出只会输出上方设置显示的键
            String name = (String) sourceAsMap.get("name");
            //由于前边设置了源文档字段过虑，这时description是取不到的
            String description = (String) sourceAsMap.get("description");
            //学习模式
            String studymodel = (String) sourceAsMap.get("studymodel");
            //价格
            Double price = (Double) sourceAsMap.get("price");
            System.out.println(name);
            System.out.println(studymodel);
            //已过滤
            System.out.println(description);
        }
        System.out.println(totalHits);
    }

    @Test
    //分页查询
    public void testSearchPage()throws IOException{
        //搜索请求对象
        SearchRequest searchRequest = new SearchRequest("xc_course");
        //类型
        searchRequest.types("doc");
        //搜索源构建对象
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //设置分页条件
        int page = 1;
        int size = 1;
        int from = (page - 1 ) * size;
        sourceBuilder.from(from);
        sourceBuilder.size(size);
        //查询所有
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        //设置需要显示的键,后面的为不用显示的
        sourceBuilder.fetchSource(new String[]{"name","studymodel","price","timestamp"},new String[]{});
        //向所有请求设置搜索源
        searchRequest.source(sourceBuilder);
        //向Es发起http请求
        SearchResponse searchResponse = highLevelClient.search(searchRequest);
        //获取到搜索结果
        SearchHits hits = searchResponse.getHits();
        //获取到个数
        long totalHits = hits.getTotalHits();
        //获取到匹配度最高的
        SearchHit[] hitsHits = hits.getHits();
        //输出
        for (SearchHit hitsHit : hitsHits) {
            Map<String, Object> sourceAsMap = hitsHit.getSourceAsMap();
            //该输出只会输出上方设置显示的键
            String name = (String) sourceAsMap.get("name");
            //由于前边设置了源文档字段过虑，这时description是取不到的
            String description = (String) sourceAsMap.get("description");
            //学习模式
            String studymodel = (String) sourceAsMap.get("studymodel");
            //价格
            Double price = (Double) sourceAsMap.get("price");
            System.out.println(name);
            System.out.println(studymodel);
            //已过滤
            System.out.println(description);
        }
        System.out.println("总记录数:" + totalHits);
    }

    @Test
    //使用Term Query精确查询和根据id查询
    public void TermQueryTest()throws IOException{
        //搜索请求对象
        SearchRequest searchRequest = new SearchRequest("xc_course");
        //类型
        searchRequest.types("doc");
        //搜索源构建对象
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        //创建id,使用id查询
        String[] ids = new String[]{"1","2"};
        //Term Query g根据id查询
        sourceBuilder.query(QueryBuilders.termsQuery("_id",ids));

        //Term Query 精确查询
        sourceBuilder.query(QueryBuilders.termQuery("name","spring"));

        //matchQuery 全文检索查询,表示必须是两个单词的占比有80%,即2*0.8=1.6 至少要有一个单词显示
        sourceBuilder.query(QueryBuilders.matchQuery("description","spring开发").minimumShouldMatch("80%"));

        //multi Query 提升权重 表示将name中有spring css 两个单词的查询结果得分提高10倍
        sourceBuilder.query(QueryBuilders.multiMatchQuery("spring css","name","description").minimumShouldMatch("80%").field("name",10));

        //boolQuery查询,可以查询多个条件
        TermQueryBuilder termQueryBuilder = QueryBuilders.termQuery("name", "spring");
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("description", "spring开发").minimumShouldMatch("80%");
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.must(termQueryBuilder);
        boolQueryBuilder.must(matchQueryBuilder);
        sourceBuilder.query(boolQueryBuilder);

        //使用过滤器查询,过滤器只对结果集进行过滤
        boolQueryBuilder.filter(QueryBuilders.termQuery("studymodel","201001"));
        //查询结果大于60小于100
        boolQueryBuilder.filter(QueryBuilders.rangeQuery("price").gte(60).lte(100));

        //设置排序 降序 注意使用sourceBuilder查询
        sourceBuilder.sort("studymodel", SortOrder.DESC);
        //升序
        sourceBuilder.sort("price", SortOrder.ASC);

        //设置需要显示的键,后面的为不用显示的
        sourceBuilder.fetchSource(new String[]{"name","studymodel","price","timestamp"},new String[]{});
        //向所有请求设置搜索源
        searchRequest.source(sourceBuilder);
        //向Es发起http请求
        SearchResponse searchResponse = highLevelClient.search(searchRequest);
        //获取到搜索结果
        SearchHits hits = searchResponse.getHits();
        //获取到个数
        long totalHits = hits.getTotalHits();
        //获取到匹配度最高的
        SearchHit[] hitsHits = hits.getHits();
        //输出
        for (SearchHit hitsHit : hitsHits) {
            Map<String, Object> sourceAsMap = hitsHit.getSourceAsMap();
            //该输出只会输出上方设置显示的键
            String name = (String) sourceAsMap.get("name");
            //由于前边设置了源文档字段过虑，这时description是取不到的
            String description = (String) sourceAsMap.get("description");
            //学习模式
            String studymodel = (String) sourceAsMap.get("studymodel");
            //价格
            Double price = (Double) sourceAsMap.get("price");
            System.out.println(name);
            System.out.println(studymodel);
            //已过滤
            System.out.println(description);
        }
        System.out.println("总记录数:" + totalHits);
    }

    @Test
    //高亮显示
    public void testHighlight()throws IOException{
        //搜索请求对象
        SearchRequest searchRequest = new SearchRequest("xc_course");
        //类型
        searchRequest.types("doc");
        //搜索源构建对象
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //查询所有
        sourceBuilder.query(QueryBuilders.matchAllQuery());
        //设置需要显示的键,后面的为不用显示的
        sourceBuilder.fetchSource(new String[]{"name","studymodel","price","timestamp"},new String[]{});
        //向所有请求设置搜索源
        searchRequest.source(sourceBuilder);
        //查询条件
        MultiMatchQueryBuilder matchQueryBuilder = QueryBuilders.multiMatchQuery("开发", "name", "description");
        sourceBuilder.query(matchQueryBuilder);
        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<tag>");
        highlightBuilder.postTags("</tag>");
        //设置高亮字段
        highlightBuilder.field(new HighlightBuilder.Field("name"));
        sourceBuilder.highlighter(highlightBuilder);

        //向Es发起http请求
        SearchResponse searchResponse = highLevelClient.search(searchRequest);
        //获取到搜索结果
        SearchHits hits = searchResponse.getHits();
        //获取到个数
        long totalHits = hits.getTotalHits();
        //获取到匹配度最高的
        SearchHit[] hitsHits = hits.getHits();
        //输出
        for (SearchHit hitsHit : hitsHits) {
            Map<String, Object> sourceAsMap = hitsHit.getSourceAsMap();
            //该输出只会输出上方设置显示的键
            String name = (String) sourceAsMap.get("name");
            //取出高亮内容
            Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
            StringBuilder sb = new StringBuilder();
            if(highlightFields != null){
                HighlightField field = highlightFields.get("name");
                if(field != null){
                    Text[] fragments = field.getFragments();
                    for (Text text : fragments) {
                        sb.append(text);
                    }
                }
                name = sb.toString();
            }
            //由于前边设置了源文档字段过虑，这时description是取不到的
            String description = (String) sourceAsMap.get("description");
            //学习模式
            String studymodel = (String) sourceAsMap.get("studymodel");
            //价格
            Double price = (Double) sourceAsMap.get("price");
            System.out.println(name);
            System.out.println(studymodel);
            //已过滤
            System.out.println(description);
        }
        System.out.println("总记录数:" + totalHits);
    }
}
