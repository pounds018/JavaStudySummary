package cn.pounds.doc;

import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.MultiGetItemResponse;
import org.elasticsearch.action.get.MultiGetRequest;
import org.elasticsearch.action.get.MultiGetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RethrottleRequest;
import org.elasticsearch.client.tasks.TaskSubmissionResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.*;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.tasks.TaskId;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MultiDocApiTest {

    private final static Logger logger = LoggerFactory.getLogger(SingleDocApiTest.class);

    private static final String INDEX_NAME = "megacorp";

    private static final RequestOptions COMMON_OPTIONS;
    static {
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder()
                // 添加全局共享header, 所有请求都会带上这部分header
                .addHeader("Authorization", "Bearer ");

        // 定义请求响应对象的jvm堆缓存, 如果数据比较大的时候就可以设置这个, 默认为100MB
        builder.setHttpAsyncResponseConsumerFactory(
                new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));

        COMMON_OPTIONS = builder.build();
    }

    @Autowired
    private RestHighLevelClient esClient;
    /**
     * 批量操作文档, 实际上就是建立了一个pipeline
     */
    @Test
    public void testBulk(){
        // 思路: 创建各种操作的request对象,然后使用bulk的request对象来包装这些个对象, 加入bulkRequest的请求可以相同类型的操作也可以是不同类型的操作

        // -------------------------------------------- 方式1 -----------------------------------------------------\\
        // 创建bulk的request对象
        BulkRequest bulkRequest = new BulkRequest();

        // 创建删除request,并添加进去
        DeleteRequest deleteRequest = new DeleteRequest(INDEX_NAME, "8");
        bulkRequest.add(deleteRequest);

        // 创建添加索引request
        Map map = new HashMap();
        map.put("name","栈");
        IndexRequest indexRequest = new IndexRequest(INDEX_NAME).id("6").source(map);
        bulkRequest.add(indexRequest);

        // 创建修改request
        Map map1 = new HashMap();
        map1.put("name","3132");
        IndexRequest indexRequest1 = new IndexRequest(INDEX_NAME).id("2").source(map1);
        bulkRequest.add(indexRequest1);

        // --------------------------------------------- 可选配置 --------------------------------------------------\\
        bulkRequest.timeout(TimeValue.timeValueMinutes(2));
        bulkRequest.timeout("2m");

        bulkRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
        bulkRequest.setRefreshPolicy("wait_for");

        bulkRequest.waitForActiveShards(2);
        bulkRequest.waitForActiveShards(ActiveShardCount.ALL);
        // 设置所有被包装的操作使用的pipelineId, 可以被具体操作的pipelineId覆盖
        bulkRequest.pipeline("pipelineId");
        // 设置所有被包装request的routing属性, 可以被具体操作的routing属性覆盖
        bulkRequest.routing("routingId");

        // 执行操作
        BulkResponse bulk = null;
        try {
            bulk = esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        bulk.forEach(bkResponse-> {
            DocWriteResponse itemResponse = bkResponse.getResponse();

            switch (bkResponse.getOpType()) {
                case INDEX:
                case CREATE:
                    IndexResponse indexResponse = (IndexResponse) itemResponse;
                    break;
                case UPDATE:
                    UpdateResponse updateResponse = (UpdateResponse) itemResponse;
                    break;
                case DELETE:
                    DeleteResponse deleteResponse = (DeleteResponse) itemResponse;
            }
        });

        // 检查批量操作是否有操作没有执行完成
        if (bulk.hasFailures()) {
            for (BulkItemResponse bulkItemResponse : bulk) {
                if (bulkItemResponse.isFailed()) {
                    BulkItemResponse.Failure failure =
                            bulkItemResponse.getFailure();
                }
            }

        }


        // ------------------------------------------ 方式2 ----------------------------------------------------\\
        // bulkProcessor是通过一个包装类简化bulk操作的类, 需要两个参数RestHighLevelClient和BulkProcessor.Listener
        BulkProcessor.Listener listener = new BulkProcessor.Listener() {
            @Override
            public void beforeBulk(long executionId, BulkRequest request) {
                int numberOfActions = request.numberOfActions();
                logger.debug("Executing bulk [{}] with {} requests",
                        executionId, numberOfActions);
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request,
                                  BulkResponse response) {
                if (response.hasFailures()) {
                    logger.warn("Bulk [{}] executed with failures", executionId);
                } else {
                    logger.debug("Bulk [{}] completed in {} milliseconds",
                            executionId, response.getTook().getMillis());
                }
            }

            @Override
            public void afterBulk(long executionId, BulkRequest request,
                                  Throwable failure) {
                logger.error("Failed to execute bulk", failure);
            }
        };

        BulkProcessor bulkProcessor =
                BulkProcessor.builder(
                                (request, bulkListener) -> esClient.bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                                listener,
                                "bulk-processor-name")
                        // 设置bulkRequest里面添加了多少request的时候开始执行bulkRequest(基于数量), -1表示关闭这个功能
                        .setBulkActions(500)
                        //  设置bulkRequest里面添加了多少request的时候开始执行bulkRequest(基于size), -1表示关闭这个功能
                        .setBulkSize(new ByteSizeValue(1L, ByteSizeUnit.MB))
                        // 设置执行的并发度, 0表示串行
                        .setConcurrentRequests(0)
                        //
                        .setFlushInterval(TimeValue.timeValueSeconds(10L))
                        .setBackoffPolicy(BackoffPolicy.constantBackoff(TimeValue.timeValueSeconds(1L), 3))
                        .build();
        // 添加请求
        IndexRequest one = new IndexRequest("posts").id("1")
                .source(XContentType.JSON, "title",
                        "In which order are my Elasticsearch queries executed?");
        IndexRequest two = new IndexRequest("posts").id("2")
                .source(XContentType.JSON, "title",
                        "Current status and upcoming changes in Elasticsearch");
        IndexRequest three = new IndexRequest("posts").id("3")
                .source(XContentType.JSON, "title",
                        "The Future of Federated Search in Elasticsearch");

        bulkProcessor.add(one);
        bulkProcessor.add(two);
        bulkProcessor.add(three);

        try {
            // 延时关闭
            boolean terminated = bulkProcessor.awaitClose(30L, TimeUnit.SECONDS);
            // 立即关闭
            bulkProcessor.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


    }

    /**
     * 并行执行多个request
     */
    @Test
    public void MultiGetRequest(){
        MultiGetRequest multiGetRequest = new MultiGetRequest();
        multiGetRequest.add(new MultiGetRequest.Item(
                "index",
                "example_id"));
        multiGetRequest.add(new MultiGetRequest.Item("index", "another_id"));

        // ----------------------------- 可选参数 -----------------------------------------------\\
        multiGetRequest.add(new MultiGetRequest.Item("index", "example_id")
                .fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE));
        String[] includes = new String[] {"foo", "*r"};
        String[] excludes = Strings.EMPTY_ARRAY;
        FetchSourceContext fetchSourceContext =
                new FetchSourceContext(true, includes, excludes);
        multiGetRequest.add(new MultiGetRequest.Item("index", "example_id")
                .fetchSourceContext(fetchSourceContext));
        includes = Strings.EMPTY_ARRAY;
        excludes = new String[] {"foo", "*r"};
        fetchSourceContext =
                new FetchSourceContext(true, includes, excludes);

        multiGetRequest.preference("some_preference");
        multiGetRequest.realtime(false);
        multiGetRequest.refresh(true);


        multiGetRequest.add(new MultiGetRequest.Item("index", "example_id")
                .fetchSourceContext(fetchSourceContext));

        multiGetRequest.add(new MultiGetRequest.Item("index", "example_id")
                .storedFields("foo"));
        MultiGetResponse response = null;
        try {
            response = esClient.mget(multiGetRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        MultiGetItemResponse item = response.getResponses()[0];
        String value = item.getResponse().getField("foo").getValue();

        multiGetRequest.add(new MultiGetRequest.Item("index", "with_routing")
                .routing("some_routing"));
        multiGetRequest.add(new MultiGetRequest.Item("index", "with_version")
                .versionType(VersionType.EXTERNAL)
                .version(10123L));
    }


    /**
     * reindex 复制索引
     * 注意: 在reindex之前要先设置好目标索引, 比如mapping, 分片数量, 副本数量
     */
    @Test
    public void testReindex(){
        ReindexRequest request = new ReindexRequest();
        // source1,2都是目标索引
        request.setSourceIndices("source1", "source2");
        request.setDestIndex("dest");

        // 1. dest元素可以像index API一样进行配置基于乐观锁的并发控制.
        // 2. 不设置version_type或者version_type设置为internal, es强制性将source index中的文档存入dest index, 覆盖具有相同类型和id的任何文档
        // 3. 设置version_type为external, 从source index复制出来doc会携带原版本号, dest index没有的doc会创建, dest index中doc版本号低于source
        // index中doc的版本号才会更新
        request.setDestVersionType(VersionType.EXTERNAL);

        // op_type为create, 会创建dest index中不存在的doc, 存在的文档可能造成版本冲突
        request.setDestOpType("create");

        // 默认情况下, 发生版本冲突会中断reindex, 可以通过设置"conflict":"proceed"忽略版本冲突继续reindex
        request.setConflicts("proceed");

        // 将满足查询条件的doc复制给dest index中
        request.setSourceQuery(new TermQueryBuilder("user","kimchy"));
        // source query 和 remoteInfo是矛盾的, 如果同时设置了这两个属性, 在查询期间会造成验证异常.
        // remoteInfo 是从其他集群查询doc然后复制.
        // 特别注意的是: 如果查询的es集群版本低于当前es集群, 使用json编写的查询语句更安全.
        String remoteHost = "";
        int remotePort = 0;
        String user = "";
        String password = "";
        request.setRemoteInfo(
                new RemoteInfo(
                        "http", remoteHost, remotePort, null,
                        new BytesArray(new MatchAllQueryBuilder().toString()),
                        user, password, Collections.emptyMap(),
                        new TimeValue(100, TimeUnit.MILLISECONDS),
                        new TimeValue(100, TimeUnit.SECONDS)
                )
        );

        // 设置doc reindex的最大数量
        request.setMaxDocs(10);

        // 默认情况下, 每个批次处理doc的数量为1000, 可以通过source batch size来控制
        request.setSourceBatchSize(100);

        // 设置管道使用ingest节点特性
        request.setDestPipeline("my_pipeline");

        // reindex同样可以使用脚本来修改doc
        request.setScript(
                new Script(
                        ScriptType.INLINE, "painless",
                        "if (ctx._source.user == 'kimchy') {ctx._source.likes++;}",
                        Collections.emptyMap()));

        // reindex也能有助于通过sliced-scroll对id进行自动并行切片. 使用setSlices来指定要用的切片
        request.setSlices(2);

        request.setScroll(TimeValue.timeValueMinutes(10));


        request.setTimeout(TimeValue.timeValueMinutes(2));
        request.setRefresh(true);

        BulkByScrollResponse bulkResponse =
                null;
        try {
            bulkResponse = esClient.reindex(request, RequestOptions.DEFAULT);

            ReindexRequest reindexRequest = new ReindexRequest();
            String sourceIndex = "";
            String destinationIndex = "";
            reindexRequest.setSourceIndices(sourceIndex);
            reindexRequest.setDestIndex(destinationIndex);
            reindexRequest.setRefresh(true);
            TaskSubmissionResponse reindexSubmission = esClient
                    .submitReindexTask(reindexRequest, RequestOptions.DEFAULT);

            String taskId = reindexSubmission.getTask();
        } catch (IOException e) {
            e.printStackTrace();
        }



        TimeValue timeTaken = bulkResponse.getTook();
        boolean timedOut = bulkResponse.isTimedOut();
        long totalDocs = bulkResponse.getTotal();
        long updatedDocs = bulkResponse.getUpdated();
        long createdDocs = bulkResponse.getCreated();
        long deletedDocs = bulkResponse.getDeleted();
        long batches = bulkResponse.getBatches();
        long noops = bulkResponse.getNoops();
        long versionConflicts = bulkResponse.getVersionConflicts();
        long bulkRetries = bulkResponse.getBulkRetries();
        long searchRetries = bulkResponse.getSearchRetries();
        TimeValue throttledMillis = bulkResponse.getStatus().getThrottled();
        TimeValue throttledUntilMillis =
                bulkResponse.getStatus().getThrottledUntil();
        List<ScrollableHitSource.SearchFailure> searchFailures =
                bulkResponse.getSearchFailures();
        List<BulkItemResponse.Failure> bulkFailures =
                bulkResponse.getBulkFailures();
    }

    @Test
    public void testUpdateByQuery(){
        UpdateByQueryRequest request = new UpdateByQueryRequest("source1", "source2");
        request.setConflicts("proceed");
        request.setQuery(new TermQueryBuilder("user", "kimchy"));
        request.setMaxDocs(10);
        request.setBatchSize(100);
        request.setPipeline("my_pipeline");
        request.setScript(
                new Script(
                        ScriptType.INLINE, "painless",
                        "if (ctx._source.user == 'kimchy') {ctx._source.likes++;}",
                        Collections.emptyMap()));
        request.setSlices(2);
        request.setScroll(TimeValue.timeValueMinutes(10));
        request.setRouting("=cat");

        request.setTimeout(TimeValue.timeValueMinutes(2));
        request.setRefresh(true);
        request.setIndicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);

        RequestOptions requestOptions = RequestOptions.DEFAULT.toBuilder().build();
        BulkByScrollResponse bulkResponse = null;
        try {
            bulkResponse = esClient.updateByQuery(request, requestOptions);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TimeValue timeTaken = bulkResponse.getTook();
        boolean timedOut = bulkResponse.isTimedOut();
        long totalDocs = bulkResponse.getTotal();
        long updatedDocs = bulkResponse.getUpdated();
        long deletedDocs = bulkResponse.getDeleted();
        long batches = bulkResponse.getBatches();
        long noops = bulkResponse.getNoops();
        long versionConflicts = bulkResponse.getVersionConflicts();
        long bulkRetries = bulkResponse.getBulkRetries();
        long searchRetries = bulkResponse.getSearchRetries();
        TimeValue throttledMillis = bulkResponse.getStatus().getThrottled();
        TimeValue throttledUntilMillis =
                bulkResponse.getStatus().getThrottledUntil();
        List<ScrollableHitSource.SearchFailure> searchFailures =
                bulkResponse.getSearchFailures();
        List<BulkItemResponse.Failure> bulkFailures =
                bulkResponse.getBulkFailures();
    }

    @Test
    public void testDeleteByQuery(){
        DeleteByQueryRequest request =
                new DeleteByQueryRequest("source1", "source2");

        request.setConflicts("proceed");
        request.setQuery(new TermQueryBuilder("user", "kimchy"));
        request.setMaxDocs(10);
        request.setBatchSize(100);
        request.setSlices(2);
        request.setScroll(TimeValue.timeValueMinutes(10));
        request.setRouting("=cat");

        // 可选参数
        request.setTimeout(TimeValue.timeValueMinutes(2));
        request.setRefresh(true);
        request.setIndicesOptions(IndicesOptions.LENIENT_EXPAND_OPEN);

        BulkByScrollResponse bulkResponse =
                null;
        try {
            bulkResponse = esClient.deleteByQuery(request, COMMON_OPTIONS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        TimeValue timeTaken = bulkResponse.getTook();
        boolean timedOut = bulkResponse.isTimedOut();
        long totalDocs = bulkResponse.getTotal();
        long deletedDocs = bulkResponse.getDeleted();
        long batches = bulkResponse.getBatches();
        long noops = bulkResponse.getNoops();
        long versionConflicts = bulkResponse.getVersionConflicts();
        long bulkRetries = bulkResponse.getBulkRetries();
        long searchRetries = bulkResponse.getSearchRetries();
        TimeValue throttledMillis = bulkResponse.getStatus().getThrottled();
        TimeValue throttledUntilMillis =
                bulkResponse.getStatus().getThrottledUntil();
        List<ScrollableHitSource.SearchFailure> searchFailures =
                bulkResponse.getSearchFailures();
        List<BulkItemResponse.Failure> bulkFailures =
                bulkResponse.getBulkFailures();
    }

    /**
     * 调整或者完全关闭正在执行的任务限流控制的api
     * 必要参数是 当前正在执行任务(比如: reindex, updateByQuery, deleteByQuery)的id
     */
    @Test
    public void testRethrottle(){
        TaskId taskId = new TaskId(UUIDs.randomBase64UUID());
        // 直接关闭限流
        RethrottleRequest request = new RethrottleRequest(taskId);

        // 指定限流qps, 下面这个就表示将taskId代表的任务限流为100 qps
        RethrottleRequest requestSpecificValue = new RethrottleRequest(taskId, 100.0f);

        try {
            esClient.reindexRethrottle(request, RequestOptions.DEFAULT);
            esClient.updateByQueryRethrottle(request, RequestOptions.DEFAULT);
            esClient.deleteByQueryRethrottle(request, RequestOptions.DEFAULT);
        }catch (Exception e){
            e.printStackTrace();
        }


    }
}
