package com.xuecheng.search.test;


import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

//es客户端java Api测试
@SpringBootTest
@RunWith(SpringRunner.class)
public class IndexTest {

    @Autowired
    RestHighLevelClient highLevelClient;

    @Autowired
    RestClient restClient;

    @Test
    //删除索引库
    public void deleteTest() throws IOException {
        DeleteIndexRequest indexRequest = new DeleteIndexRequest("xc_course");
        //创建客户端
        DeleteIndexResponse delete = highLevelClient.indices().delete(indexRequest);
        //接受结果
        boolean acknowledged = delete.isAcknowledged();
        System.out.println(acknowledged);
    }

    @Test
    //创建索引库
    public void createIndexTest() throws IOException {
        CreateIndexRequest indexRequest = new CreateIndexRequest("xc_course");
        //设置索引参数 分片 和 副本的数量
        indexRequest.settings(Settings.builder().put("number_of_shards", "1").put("number_of_replicas", "0"));
        //创建映射
        indexRequest.mapping("doc", "{\n" +
                "    \"properties\": {\n" +
                "        \"name\": {\n" +
                "            \"type\": \"text\"\n" +
                "        },\n" +
                "        \"description\": {\n" +
                "            \"type\": \"text\"\n" +
                "        },\n" +
                "        \"studymodel\": {\n" +
                "            \"type\": \"keyword\"\n" +
                "        }\n" +
                "    }\n" +
                "}", XContentType.JSON);

        //创建客户端
        IndicesClient indices = highLevelClient.indices();
        //创建
        CreateIndexResponse createIndexResponse = indices.create(indexRequest);
        //接受结果
        boolean acknowledged = createIndexResponse.isShardsAcknowledged();
        System.out.println(acknowledged);
    }

    @Test
    //添加文档
    public void addDocTest() throws IOException {
        //准备json数据
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("name", "spring cloud实战");
        jsonMap.put("description", "本课程主要从四个章节进行讲解： 1.微服务架构入门 2.spring cloud 基础入门 3.实战Spring Boot 4.注册中心eureka。");
        jsonMap.put("studymodel", "201001");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy‐MM‐dd HH:mm:ss");
        jsonMap.put("timestamp", dateFormat.format(new Date()));
        jsonMap.put("price", 5.6f);
        //索引请求对象
        IndexRequest indexRequest = new IndexRequest("xc_course", "doc");
        indexRequest.source(jsonMap);
        //索引响应对象
        IndexResponse indexResponse = highLevelClient.index(indexRequest);
        //获取结果集
        DocWriteResponse.Result result = indexResponse.getResult();
        System.out.println(result);
    }

    @Test
    //删除文档
    public void deleteDoc() throws IOException {
        String id = "ZcyRLmwB2mcUF0l-A47-";
        //删除索引请求对象
        DeleteRequest deleteRequest = new DeleteRequest("xc_course", "doc", id);
        //响应对象
        DeleteResponse delete = highLevelClient.delete(deleteRequest);
        //获取响应结果
        DocWriteResponse.Result result = delete.getResult();
        System.out.println(result);
    }

    @Test
    //修改文档
    public void udateDoc() throws IOException {
        //给与查询条件
        UpdateRequest updateRequest = new UpdateRequest("xc_course", "doc", "ZcyRLmwB2mcUF0l-A47-");
        //修改数据
        Map<String, String> map = new HashMap<>();
        map.put("name", "spring cloud实战");
        //重新添加到doc中
        UpdateRequest update = updateRequest.doc(map);
        //获取到索引请求对象
        UpdateResponse update1 = highLevelClient.update(update);
        //获得结果
        RestStatus status = update1.status();
        System.out.println(status);
    }

    @Test
    //查询文档
    public void getDoc() throws IOException {
        //给与查询条件
        GetRequest getRequest = new GetRequest("xc_course", "doc", "ZcyRLmwB2mcUF0l-A47-");
        //获取到响应对象
        GetResponse documentFields = highLevelClient.get(getRequest);
        //接受结果集
        Map<String, Object> source = documentFields.getSource();
        System.out.println(source);
    }
}
