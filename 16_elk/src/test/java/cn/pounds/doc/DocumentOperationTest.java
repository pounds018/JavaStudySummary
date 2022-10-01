package cn.pounds.doc;

import cn.pounds.pojo.Person;
import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @Project: 07_elasticsearch
 * @Date: 2020/10/29 11:03
 * @author: by Martin
 * @Description: elasticsearch 文档的增删改查查
 */
@SpringBootTest
public class DocumentOperationTest {
    @Autowired
    private RestHighLevelClient esClient;

    @Test
    public void createIndexTest() throws IOException {
         // 获取索引操作对象
        IndicesClient indices = esClient.indices();
        // 获取创建索引请求对象
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("itcast");

        // 带映射的方式创建索引
        String mappings = "{\n" +
                "      \"properties\" : {\n" +
                "        \"address\" : {\n" +
                "          \"type\" : \"text\",\n" +
                "          \"analyzer\" : \"ik_max_word\"\n" +
                "        },\n" +
                "        \"age\" : {\n" +
                "          \"type\" : \"integer\"\n" +
                "        },\n" +
                "        \"name\" : {\n" +
                "          \"type\" : \"keyword\"\n" +
                "        }\n" +
                "      }\n" +
                "    }";
//         将mappings封装进请求里面
        createIndexRequest.mapping(mappings, XContentType.JSON);
        // 执行索引创建
        CreateIndexResponse createIndexResponse = indices.create(createIndexRequest, RequestOptions.DEFAULT);
        // 获取请求响应的结果
        System.out.println(createIndexResponse.isAcknowledged());
    }

    /**
     * 添加文档有两种常用方式来准备数据,map或者pojo
     * 添加文档和修改文档都是使用这个操作,
     *      只是如果id存在的时候,就是修改数据
     *      如果id不存在,就是添加操作
     * @throws IOException
     */
    @Test
    public void addDocumentTest() throws IOException {
        // 准备数据,方式1
        Map map = new HashMap<>();
        map.put("name","阿尔萨斯");
        map.put("address","诺森德");
        map.put("age",15);
        // 获取操作文档的对象
        IndexRequest indexRequest = new IndexRequest("itcast").id("3").source(map);
        // 添加数据
        IndexResponse indexResponse = esClient.index(indexRequest, RequestOptions.DEFAULT);
        System.out.println(indexResponse.getResult());


        // 准备数据方式2
        Person person = new Person(2, "成都", 25, "吹屎泡儿");
        String jsonString = JSON.toJSONString(person);

        IndexResponse indexResponse1 =
                esClient.index(new IndexRequest("itcast").id(person.getId().toString()).source(jsonString,XContentType.JSON),
                RequestOptions.DEFAULT);

        System.out.println(indexResponse1.getId());



    }

    /**
     * 查询文档
     */
    @Test
    public void findDocByIdTest() throws IOException{
        GetRequest itcast = new GetRequest("megacorp", "3");
        GetResponse documentFields = esClient.get(itcast, RequestOptions.DEFAULT);

        System.out.println(documentFields.getSourceAsString());
    }

    @Test
    public void delDocByIdTest() throws IOException{
        DeleteRequest itcast = new DeleteRequest("itcast", "3");
        DeleteResponse documentFields = esClient.delete(itcast, RequestOptions.DEFAULT);

        System.out.println(documentFields.getResult());
    }

    @Test
    public void updateDocTest() throws Exception {
        UpdateRequest updateRequest = new UpdateRequest("megacorp", "1");
        updateRequest.doc(XContentType.JSON, "age", 26);
        UpdateResponse updateResponse = esClient.update(updateRequest, RequestOptions.DEFAULT);
        System.out.println(updateResponse.getResult());
        esClient.close();
    }
    @Test
    public void updateDocTest1() throws Exception {
        UpdateRequest updateRequest = new UpdateRequest("megacorp", "1");
        updateRequest.doc(XContentType.JSON, "age", 26);
        UpdateResponse updateResponse = esClient.update(updateRequest, RequestOptions.DEFAULT);
        System.out.println(updateResponse.getResult());
        esClient.close();
    }
}
