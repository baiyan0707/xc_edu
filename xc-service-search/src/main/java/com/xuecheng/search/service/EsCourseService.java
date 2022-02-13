package com.xuecheng.search.service;


import com.xuecheng.framework.domain.course.CoursePub;
import com.xuecheng.framework.domain.course.response.TeachplanMediaPub;
import com.xuecheng.framework.domain.search.CourseSearchParam;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EsCourseService {

    @Value("${xuecheng.course.index}")
    private String index;
    @Value("${xuecheng.media.index}")
    private String media_index;
    @Value("${xuecheng.course.type}")
    private String type;
    @Value("${xuecheng.media.type}")
    private String media_type;
    @Value("${xuecheng.course.source_field}")
    private String source_field;
    @Value("${xuecheng.media.source_field}")
    private String media_source_field;

    @Autowired
    RestHighLevelClient highLevelClient;

    /**
     * 课程搜索
     * @param page
     * @param size
     * @param courseSearchParam
     * @return
     */
    public QueryResponseResult<CoursePub> list(int page, int size, CourseSearchParam courseSearchParam) {
        if (courseSearchParam == null) {
            courseSearchParam = new CourseSearchParam();
        }
        //设置索引和类型
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.types(type);
        //创建搜索源
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //设置过滤字段
        String[] source_fields = source_field.split(",");
        searchSourceBuilder.fetchSource(source_fields, new String[]{});
        //判断关键字如果不为空
        if (StringUtils.isNotEmpty(courseSearchParam.getKeyword())) {
            //查询条件为关键字查询
            MultiMatchQueryBuilder matchQueryBuilder = QueryBuilders.multiMatchQuery(courseSearchParam.getKeyword(), "name", "teachplan", "description")
                    .minimumShouldMatch("70%").field("name", 10);

            boolQueryBuilder.must(matchQueryBuilder);
        }
        //一级分类
        if (StringUtils.isNotEmpty(courseSearchParam.getMt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("mt", courseSearchParam.getMt()));
        }
        //贰级分类
        if (StringUtils.isNotEmpty(courseSearchParam.getSt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("st", courseSearchParam.getSt()));
        }
        //等级描述
        if (StringUtils.isNotEmpty(courseSearchParam.getGrade())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade", courseSearchParam.getGrade()));
        }
        //使用布尔查询
        searchSourceBuilder.query(boolQueryBuilder);
        //设置分页条件
        if (page <= 0) {
            page = 1;
        }
        int from = (page - 1) * size;
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);
        //设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font class='eslight'>");
        highlightBuilder.postTags("</font>");
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
        searchSourceBuilder.highlighter(highlightBuilder);
        //请求搜索
        searchRequest.source(searchSourceBuilder);

        //创建接受数据的对象
        QueryResult<CoursePub> queryResult = new QueryResult<>();
        List<CoursePub> list = new ArrayList<>();
        SearchResponse search = null;
        try {
            //执行搜索
            search = highLevelClient.search(searchRequest);
            //获取响应结果
            SearchHits hits = search.getHits();
            long total = hits.getTotalHits();
            //记录总数
            queryResult.setTotal(total);
            SearchHit[] hitsHits = hits.getHits();
            for (SearchHit hitsHit : hitsHits) {
                CoursePub coursePub = new CoursePub();
                //取出结果并且赋值
                Map<String, Object> asMap = hitsHit.getSourceAsMap();
                //名称
                String name = (String) asMap.get("name");
                coursePub.setName(name);
                //id
                String id = (String) asMap.get("id");
                coursePub.setId(id);
                //取出高亮内容
                Map<String, HighlightField> highlightFields = hitsHit.getHighlightFields();
                if (highlightFields != null) {
                    HighlightField highlightField = highlightFields.get("name");
                    if (highlightField != null) {
                        Text[] texts = highlightField.fragments();
                        StringBuffer sb = new StringBuffer();
                        for (Text text : texts) {
                            sb.append(text);
                        }
                        name = sb.toString();
                    }
                    coursePub.setName(name);
                }
                //图片
                String pic = (String) asMap.get("pic");
                coursePub.setPic(pic);
                Double price = null;
                try {
                    if (asMap.get("price") != null) {
                        price = (Double) asMap.get("price");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //价格
                coursePub.setPrice(price);
                Double price_old = null;
                try {
                    if (asMap.get("price_old") != null) {
                        price_old = (Double) asMap.get("price_old");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //之前的价格
                coursePub.setPrice_old(price_old);
                list.add(coursePub);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        //赋值并且返回
        queryResult.setList(list);
        QueryResponseResult<CoursePub> result = new QueryResponseResult<>(CommonCode.SUCCESS, queryResult);
        return result;
    }

    /**
     * 根据id查询课程信息
     * @param id
     * @return
     */
    public Map<String, CoursePub> getall(String id) {
        //创建一个es搜索,设置索引库和类型
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.types(type);
        //使用termQuery搜索
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("id", id));
        searchRequest.source(searchSourceBuilder);
        //创建map用于返回数据
        Map<String, CoursePub> map = new HashMap<>();
        SearchResponse search = null;
        try {
            //执行搜索
            search = highLevelClient.search(searchRequest);
            SearchHits hits = search.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit searchHit : searchHits) {
                Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
                //创建对象并且给对象赋值
                CoursePub coursePub = new CoursePub();
                String courseId = (String) sourceAsMap.get("id");
                String name = (String) sourceAsMap.get("name");
                String grade = (String) sourceAsMap.get("grade");
                String charge = (String) sourceAsMap.get("charge");
                String pic = (String) sourceAsMap.get("pic");
                String description = (String) sourceAsMap.get("description");
                String teachplan = (String) sourceAsMap.get("teachplan");
                coursePub.setId(courseId);
                coursePub.setName(name);
                coursePub.setPic(pic);
                coursePub.setGrade(grade);
                coursePub.setTeachplan(teachplan);
                coursePub.setDescription(description);
                map.put(id,coursePub);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    /**
     * 根据多个id查询媒资信息
     * @param teachplanIds
     * @return
     */
    public QueryResponseResult<TeachplanMediaPub> getmedia(String[] teachplanIds) {
        //创建一个es搜索,设置索引库和类型
        SearchRequest searchRequest = new SearchRequest(media_index);
        searchRequest.types(media_type);
        //使用termsQuery搜索
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termsQuery("teachplan_id", teachplanIds));
        //设置过滤
        String[] includes = media_source_field.split(",");
        searchSourceBuilder.fetchSource(includes,new String[]{});
        searchRequest.source(searchSourceBuilder);

        //创建对象,用来接受结果值
        List<TeachplanMediaPub> mediaPubList = new ArrayList<>();
        TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
        long total = 0;
        SearchResponse search = null;

        try {
            //使用es客户端进行搜索请求Es
            search = highLevelClient.search(searchRequest);
            SearchHits hits = search.getHits();
            total = hits.totalHits;
            SearchHit[] hitsHits = hits.getHits();
            for (SearchHit hitsHit : hitsHits) {
                Map<String, Object> map = hitsHit.getSourceAsMap();
                //取出课程计划媒资信息
                String courseid = (String) map.get("courseid");
                String media_id = (String) map.get("media_id");
                String media_url = (String) map.get("media_url");
                String teachplan_id = (String) map.get("teachplan_id");
                String media_fileoriginalname = (String) map.get("media_fileoriginalname");

                //添加数据并保存
                teachplanMediaPub.setCourseId(courseid);
                teachplanMediaPub.setMediaUrl(media_url);
                teachplanMediaPub.setMediaFileOriginalName(media_fileoriginalname);
                teachplanMediaPub.setMediaId(media_id);
                teachplanMediaPub.setTeachplanId(teachplan_id);
                mediaPubList.add(teachplanMediaPub);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //数据集合并返回
        QueryResult<TeachplanMediaPub> queryResult = new QueryResult<>();
        queryResult.setTotal(total);
        queryResult.setList(mediaPubList);
        QueryResponseResult<TeachplanMediaPub> result = new QueryResponseResult<TeachplanMediaPub>(CommonCode.SUCCESS,queryResult);
        return result;
    }
}
