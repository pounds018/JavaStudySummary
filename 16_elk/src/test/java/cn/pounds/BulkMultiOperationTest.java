package cn.pounds;

import cn.pounds.domain.Goods;
import cn.pounds.mapper.GoodsMapper;
import com.alibaba.fastjson.JSON;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Project: 07_elasticsearch
 * @Date: 2020/10/29 16:12
 * @author: by Martin
 * @Description: TODO
 */
@SpringBootTest
public class BulkMultiOperationTest {
    @Resource
    private RestHighLevelClient esClient;
    @Resource
    private GoodsMapper goodsMapper;

    /**
     * # 批量操作演示:
     * # 需求:
     * # 1. 删除编号为8的文档
     * # 2. 添加编号为6的文档
     * # 3. 修改编号为3的文档,"age"改为 25
     */
    @Test
    public void multiOperationTest() throws IOException {
        // 思路: 创建各种操作的request对象,然后使用bulk的request对象来包装这些个对象

        // 创建bulk的request对象
        BulkRequest bulkRequest = new BulkRequest();

        // 创建删除request,并添加进去
        DeleteRequest deleteRequest = new DeleteRequest("itcast", "8");
        bulkRequest.add(deleteRequest);

        // 创建添加索引request
        Map map = new HashMap();
        map.put("name","栈");
        IndexRequest indexRequest = new IndexRequest("itcast").id("6").source(map);
        bulkRequest.add(indexRequest);

        // 创建修改request
        Map map1 = new HashMap();
        map1.put("name","3132");
        IndexRequest indexRequest1 = new IndexRequest("itcast").id("2").source(map1);
        bulkRequest.add(indexRequest1);

        // 执行操作
        BulkResponse bulk = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        bulk.forEach(bkResponse-> System.out.println(bkResponse.status()));
    }

    /**
     * 从数据库中查询出数据,然后存入es中
     */
    @Test
    public void importFromDBTest() throws IOException {
        List<Goods> goods = goodsMapper.selectList(null);
        // 创建批量操作的对象
        BulkRequest bulkRequest = new BulkRequest();

        for (Goods good : goods) {
            // 将数据库中规格信息封装成Map,存放进good的spec属性里面
            Map map = JSON.parseObject(good.getSpecDatabaseStr(), Map.class);
            good.setSpec(map);

            IndexRequest indexRequest =
                    new IndexRequest("goods").id(good.getId().toString()).source(JSON.toJSONString(good), XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulkResponse = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);

    }
}
