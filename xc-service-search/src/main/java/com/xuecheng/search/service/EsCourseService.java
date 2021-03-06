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
     * ????????????
     * @param page
     * @param size
     * @param courseSearchParam
     * @return
     */
    public QueryResponseResult<CoursePub> list(int page, int size, CourseSearchParam courseSearchParam) {
        if (courseSearchParam == null) {
            courseSearchParam = new CourseSearchParam();
        }
        //?????????????????????
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.types(type);
        //???????????????
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        //??????????????????
        String[] source_fields = source_field.split(",");
        searchSourceBuilder.fetchSource(source_fields, new String[]{});
        //??????????????????????????????
        if (StringUtils.isNotEmpty(courseSearchParam.getKeyword())) {
            //??????????????????????????????
            MultiMatchQueryBuilder matchQueryBuilder = QueryBuilders.multiMatchQuery(courseSearchParam.getKeyword(), "name", "teachplan", "description")
                    .minimumShouldMatch("70%").field("name", 10);

            boolQueryBuilder.must(matchQueryBuilder);
        }
        //????????????
        if (StringUtils.isNotEmpty(courseSearchParam.getMt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("mt", courseSearchParam.getMt()));
        }
        //????????????
        if (StringUtils.isNotEmpty(courseSearchParam.getSt())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("st", courseSearchParam.getSt()));
        }
        //????????????
        if (StringUtils.isNotEmpty(courseSearchParam.getGrade())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("grade", courseSearchParam.getGrade()));
        }
        //??????????????????
        searchSourceBuilder.query(boolQueryBuilder);
        //??????????????????
        if (page <= 0) {
            page = 1;
        }
        int from = (page - 1) * size;
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(size);
        //????????????
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<font class='eslight'>");
        highlightBuilder.postTags("</font>");
        highlightBuilder.fields().add(new HighlightBuilder.Field("name"));
        searchSourceBuilder.highlighter(highlightBuilder);
        //????????????
        searchRequest.source(searchSourceBuilder);

        //???????????????????????????
        QueryResult<CoursePub> queryResult = new QueryResult<>();
        List<CoursePub> list = new ArrayList<>();
        SearchResponse search = null;
        try {
            //????????????
            search = highLevelClient.search(searchRequest);
            //??????????????????
            SearchHits hits = search.getHits();
            long total = hits.getTotalHits();
            //????????????
            queryResult.setTotal(total);
            SearchHit[] hitsHits = hits.getHits();
            for (SearchHit hitsHit : hitsHits) {
                CoursePub coursePub = new CoursePub();
                //????????????????????????
                Map<String, Object> asMap = hitsHit.getSourceAsMap();
                //??????
                String name = (String) asMap.get("name");
                coursePub.setName(name);
                //id
                String id = (String) asMap.get("id");
                coursePub.setId(id);
                //??????????????????
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
                //??????
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
                //??????
                coursePub.setPrice(price);
                Double price_old = null;
                try {
                    if (asMap.get("price_old") != null) {
                        price_old = (Double) asMap.get("price_old");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                //???????????????
                coursePub.setPrice_old(price_old);
                list.add(coursePub);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        //??????????????????
        queryResult.setList(list);
        QueryResponseResult<CoursePub> result = new QueryResponseResult<>(CommonCode.SUCCESS, queryResult);
        return result;
    }

    /**
     * ??????id??????????????????
     * @param id
     * @return
     */
    public Map<String, CoursePub> getall(String id) {
        //????????????es??????,????????????????????????
        SearchRequest searchRequest = new SearchRequest(index);
        searchRequest.types(type);
        //??????termQuery??????
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termQuery("id", id));
        searchRequest.source(searchSourceBuilder);
        //??????map??????????????????
        Map<String, CoursePub> map = new HashMap<>();
        SearchResponse search = null;
        try {
            //????????????
            search = highLevelClient.search(searchRequest);
            SearchHits hits = search.getHits();
            SearchHit[] searchHits = hits.getHits();
            for (SearchHit searchHit : searchHits) {
                Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
                //?????????????????????????????????
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
     * ????????????id??????????????????
     * @param teachplanIds
     * @return
     */
    public QueryResponseResult<TeachplanMediaPub> getmedia(String[] teachplanIds) {
        //????????????es??????,????????????????????????
        SearchRequest searchRequest = new SearchRequest(media_index);
        searchRequest.types(media_type);
        //??????termsQuery??????
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.termsQuery("teachplan_id", teachplanIds));
        //????????????
        String[] includes = media_source_field.split(",");
        searchSourceBuilder.fetchSource(includes,new String[]{});
        searchRequest.source(searchSourceBuilder);

        //????????????,?????????????????????
        List<TeachplanMediaPub> mediaPubList = new ArrayList<>();
        TeachplanMediaPub teachplanMediaPub = new TeachplanMediaPub();
        long total = 0;
        SearchResponse search = null;

        try {
            //??????es???????????????????????????Es
            search = highLevelClient.search(searchRequest);
            SearchHits hits = search.getHits();
            total = hits.totalHits;
            SearchHit[] hitsHits = hits.getHits();
            for (SearchHit hitsHit : hitsHits) {
                Map<String, Object> map = hitsHit.getSourceAsMap();
                //??????????????????????????????
                String courseid = (String) map.get("courseid");
                String media_id = (String) map.get("media_id");
                String media_url = (String) map.get("media_url");
                String teachplan_id = (String) map.get("teachplan_id");
                String media_fileoriginalname = (String) map.get("media_fileoriginalname");

                //?????????????????????
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
        //?????????????????????
        QueryResult<TeachplanMediaPub> queryResult = new QueryResult<>();
        queryResult.setTotal(total);
        queryResult.setList(mediaPubList);
        QueryResponseResult<TeachplanMediaPub> result = new QueryResponseResult<TeachplanMediaPub>(CommonCode.SUCCESS,queryResult);
        return result;
    }
}
