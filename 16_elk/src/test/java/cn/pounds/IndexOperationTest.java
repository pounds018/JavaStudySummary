package cn.pounds;

import org.apache.http.HttpHost;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.IndicesClient;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;


@SpringBootTest
class IndexOperationTest {
    @Autowired
    private RestHighLevelClient esClient;

    @Test
    void contextLoads() {
        // es单节点就是这么操作
        RestHighLevelClient esClient = new RestHighLevelClient(RestClient.builder(
                new HttpHost("192.168.200.128",9200,"http")
        ));

        System.out.println(esClient);
    }

    @Test
    public void testSpringbootES(){
        System.out.println(esClient);
    }

    @Test
    public void operateIndex(){
        // 1.通过es客户端拿到索引
        // 2.具体操作
    }


    /**索引的添加两种方式*/

    @Test
    public void testAddIndex() throws IOException {
        // 通过es客户端获取索引操作对象
        IndicesClient indicesClient = esClient.indices();
        /**
         * 创建索引不带映射
         * create(org.elasticsearch.action.admin.indices.create.CreateIndexRequest createIndexRequest
         * , RequestOptions options)
         */
        CreateIndexRequest createIndexRequest = new CreateIndexRequest("北京");
//        /**
//         * 创建索引带mapping,需要增加下面两条语句
//         */
//        String mappings = "{\n" +
//                "      \"properties\" : {\n" +
//                "        \"address\" : {\n" +
//                "          \"type\" : \"text\",\n" +
//                "          \"analyzer\" : \"ik_max_word\"\n" +
//                "        },\n" +
//                "        \"age\" : {\n" +
//                "          \"type\" : \"integer\"\n" +
//                "        },\n" +
//                "        \"name\" : {\n" +
//                "          \"type\" : \"keyword\"\n" +
//                "        }\n" +
//                "      }\n" +
//                "    }";
//        createIndexRequest.mapping(mappings, XContentType.JSON);
        CreateIndexResponse createIndexResponse = indicesClient.create(createIndexRequest, RequestOptions.DEFAULT);

        // 3.根据返回值判断结果
        System.out.println(createIndexResponse.isAcknowledged());
    }

    /**
     * 查询索引操作
     */
    @Test
    public void queryTest() throws IOException {
        IndicesClient indices = esClient.indices();
        GetIndexRequest getIndexRequest = new GetIndexRequest("北京");
        try {
            GetIndexResponse getIndexResponse = indices.get(getIndexRequest, RequestOptions.DEFAULT);
            getIndexResponse.getMappings().forEach((k,v)-> System.out.println(k+"--->"+v));
        }catch (Exception e) {
            System.out.println(String.format("出现异常%s",e.getMessage()));
        }
    }

    /**
     * 删除索引操作
     */
    @Test
    public void deleteTest() throws IOException {
        IndicesClient indices = esClient.indices();
        DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest("北京");
        AcknowledgedResponse acknowledgedResponse = indices.delete(deleteIndexRequest, RequestOptions.DEFAULT);

        System.out.println(acknowledgedResponse.isAcknowledged());
    }


    @Test
    public void isExistsTest() throws IOException {
        IndicesClient indices = esClient.indices();
        GetIndexRequest getIndexRequest = new GetIndexRequest("成都");
        boolean exists = indices.exists(getIndexRequest, RequestOptions.DEFAULT);

        System.out.println(exists);
    }
}
