package cn.pounds.doc;

import cn.pounds.pojo.Person;
import com.alibaba.fastjson.JSON;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.DocWriteResponse;
import org.elasticsearch.action.bulk.*;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.*;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.ActiveShardCount;
import org.elasticsearch.action.support.IndicesOptions;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.action.support.replication.ReplicationResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.HttpAsyncResponseConsumerFactory;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RethrottleRequest;
import org.elasticsearch.client.core.GetSourceRequest;
import org.elasticsearch.client.core.GetSourceResponse;
import org.elasticsearch.client.core.TermVectorsRequest;
import org.elasticsearch.client.core.TermVectorsResponse;
import org.elasticsearch.client.tasks.TaskSubmissionResponse;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.UUIDs;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.get.GetResult;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.reindex.*;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.fetch.subphase.FetchSourceContext;
import org.elasticsearch.tasks.TaskId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author: pounds
 * @date: 2022/3/7 23:38
 * @desc:
 */
@RunWith(SpringRunner.class)
@SpringBootTest("PoundsApplication.class")
public class SingleDocApiTest {
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
	 * 索引api: 添加或者更新文档
	 * public final IndexResponse index(IndexRequest indexRequest, RequestOptions options)
	 * 根据索引更新或者修改文档, 指定id的文档存在就是修改操作
	 */
	@Test
	public void test(){
		IndexRequest request = new IndexRequest("megacorp");
		request.id("1");
		//  ------------------------------- 构造文档的几种方式 --------------------------------------------\\
		// 1. json字符串
		String jsonString = "{" +
				"\"user\":\"kimchy\"," +
				"\"postDate\":\"2013-01-30\"," +
				"\"message\":\"trying out Elasticsearch\"" +
				"}";
		request.source(jsonString, XContentType.JSON);

		Person person = new Person(2, "成都", 25, "吹屎泡儿");
		jsonString = JSON.toJSONString(person);
		request.source(jsonString, XContentType.JSON);

		// 2. map
		Map<String, Object> jsonMap = new HashMap<>();
		jsonMap.put("user", "kimchy");
		jsonMap.put("postDate", new Date());
		jsonMap.put("message", "trying out Elasticsearch");

		// 3. 内置的xContentBuilder对象构造文档
		XContentBuilder builder = null;
		try {
			builder = XContentFactory.jsonBuilder();
			builder.startObject();
			{
				builder.field("user", "kimchy");
				builder.timeField("postDate", new Date());
				builder.field("message", "trying out Elasticsearch");
			}
			builder.endObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
		request.source(builder);

		// 4. 直接以kev键值对的形式作为参数传给source
		request.source("user", "kimchy",
				"postDate", new Date(),
				"message", "trying out Elasticsearch");

		// -------------------------------- index请求的可选选项 --------------------------------------------- \\
		// 设置文档的routing属性, 控制文档路由到哪一个分片上
		request.routing("routing");
		// 设置等待主分片数据可用的超时时间, 两种方式2选1
		request.timeout(TimeValue.timeValueMillis(1));
		request.timeout("1s");

		// 设置刷新策略, 两种方式2选1
		request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
		request.setRefreshPolicy("wait_for");

		// 设置文档版本, 以及版本类型
		request.version(2);
		request.versionType(VersionType.EXTERNAL);

		// 设置文档的操作方式, 创建(添加), 索引(添加/更新), 更新, 删除, 字符串只能是create或者index
		// opType为create的时候,版本冲突会抛出异常
		request.opType(DocWriteRequest.OpType.CREATE);
		request.opType("create");

		// 设置ingest节点的pipeline, 做文档预处理, 参数是pipeline的名字
		request.setPipeline("pipeline");

		// -------------------------------- 执行请求 -------------------------------------\\
		IndexResponse indexResponse = null;
		// 同步
		try {
			indexResponse = esClient.index(request, RequestOptions.DEFAULT);
		} catch (ElasticsearchException | IOException e) {
			if (e instanceof ElasticsearchException){
				ElasticsearchException exception = (ElasticsearchException) e;
				if (exception.status() == RestStatus.CONFLICT) {

				}
			}
			e.printStackTrace();
		}

		// 异步
		ActionListener<IndexResponse> listener = new ActionListener<IndexResponse>() {
			@Override
			public void onResponse(IndexResponse indexResponse) {
				// 执行成功的逻辑
				System.out.println("成功");
			}

			@Override
			public void onFailure(Exception e) {
				if (e instanceof ElasticsearchException){
					ElasticsearchException exception = (ElasticsearchException) e;
					if (exception.status() == RestStatus.CONFLICT) {

					}
				}
				// 失败的逻辑
				System.out.println("失败");
			}
		};
		esClient.indexAsync(request, RequestOptions.DEFAULT, listener);


		// ----------------------------------- 响应结果 --------------------------------------------\\
		// 可以从响应结果中获取相关信息
		String index = indexResponse.getIndex();
		String id = indexResponse.getId();
		// 判断是更新还是添加
		if (indexResponse.getResult() == DocWriteResponse.Result.CREATED) {

		} else if (indexResponse.getResult() == DocWriteResponse.Result.UPDATED) {

		}
		// 获取文档所在分片的信息
		ReplicationResponse.ShardInfo shardInfo = indexResponse.getShardInfo();
		// 判断是否所有分片都成功写入
		if (shardInfo.getTotal() != shardInfo.getSuccessful()) {
		}
		// 对失败分片原因进行处理
		if (shardInfo.getFailed() > 0) {
			for (ReplicationResponse.ShardInfo.Failure failure :
					shardInfo.getFailures()) {
				String reason = failure.reason();
			}
		}
	}

	/**
	 * 查询文档的api
	 */
	@Test
	public void testGet(){
		// 两个参数 1. index, 2. 文档id
		GetRequest request = new GetRequest(INDEX_NAME, "10086");

		// -------------------------------- 请求的可选选项 --------------------------------------------- \\
		// 允许没有文档没有内容, 默认是必须要fetch source的
		request.fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE);
		// 指定需要查询的字段
		String[] includes = new String[]{"first_name", "age"};
		String[] excludes = Strings.EMPTY_ARRAY;
		FetchSourceContext fetchSourceContext =
				new FetchSourceContext(true, includes, excludes);
		request.fetchSourceContext(fetchSourceContext);

		// 设置路由信息
		request.routing("routing");

		//
		request.preference("preference");

		 //将realtime标记设置为false(默认为true)
        request.realtime(false);
		//在检索文档之前执行刷新(默认为false)
        request.refresh(true);
        // 指定版本
		request.version(2);
		// 指定版本类型
        request.versionType(VersionType.EXTERNAL);
		// ---------------------------------- 执行方式 ------------------------------------------------- \\
		// 同步
		GetResponse getResponse = null;
		try {
			//为特定的存储字段配置检索(要求字段在映射中单独存储)
			request.storedFields("message");
			getResponse = esClient.get(request, RequestOptions.DEFAULT);
			String message = getResponse.getField("message").getValue();
		} catch (IOException e) {
			e.printStackTrace();

		}
		// 异步
		esClient.getAsync(request, RequestOptions.DEFAULT, new ActionListener<GetResponse>(){
			@Override
			public void onResponse(GetResponse documentFields) {
				System.out.println(documentFields);
			}

			@Override
			public void onFailure(Exception e) {
				ElasticsearchException exception = (ElasticsearchException) e;
				// ----------- 可能出现的两种异常 ------------------ \\\
				// 1. 版本冲突, 出现的情况为: 查询指定版本的数据, 但是当前文档版本不是指定版本
				if (exception.status() == RestStatus.CONFLICT) {

				}
				// 2. 数据不存在.
				if (exception.status() == RestStatus.NOT_FOUND) {
					System.out.println("数据不存在");
				}
			}
		});

		// -------------------------------- 响应结果 ------------------------------------------------- \\
		String index = getResponse.getIndex();
		String id = getResponse.getId();
		if (getResponse.isExists()) {
			long version = getResponse.getVersion();
			String sourceAsString = getResponse.getSourceAsString();
			Map<String, Object> sourceAsMap = getResponse.getSourceAsMap();
			byte[] sourceAsBytes = getResponse.getSourceAsBytes();
		} else {
			System.out.println("数据不存在");
		}
	}

	/**
	 * 仅仅获取doc 中_source字段
	 */
	@Test
	public void testDocSourceOnly(){
		GetSourceRequest getSourceRequest = new GetSourceRequest(INDEX_NAME, "1");

		// -------------------------------- 请求的可选选项 --------------------------------------------- \\
		String[] includes = Strings.EMPTY_ARRAY;
		String[] excludes = new String[]{"age"};
		// FetchSourceContext的第一个参数一定要是true,第二个参数是查询结果包含什么source字段, 第三个参数是查询结果排除什么source字段
		getSourceRequest.fetchSourceContext(new FetchSourceContext(true, includes, excludes));
		// 路由参数, 控制查询哪个分片
		getSourceRequest.routing("routing");
		//
		getSourceRequest.preference("preference");

		getSourceRequest.realtime(false);
		// 在获取结果之前先执行一次刷新, 默认是false
		getSourceRequest.refresh(true);


		// ---------------------------------- 执行方式, 只给出了同步方式 ------------------------------------------------- \\
		GetSourceResponse response = null;
		try {
			response = esClient.getSource(getSourceRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// -------------------------------- 响应结果 ------------------------------------------------- \\
		Map<String, Object> source = response.getSource();
	}

	/**
	 * 如何只判断某个文档是否存在
	 *
	 */
	@Test
	public void testExits(){
		GetRequest getRequest = new GetRequest(INDEX_NAME, "1");
		// 关闭获取source
		getRequest.fetchSourceContext(new FetchSourceContext(false));
		// 关闭获取stored字段
		getRequest.storedFields("_none_");

		try {
			boolean exists = esClient.exists(getRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 删除操作
	 */
	@Test
	public void testDelete(){
		DeleteRequest deleteRequest = new DeleteRequest(INDEX_NAME, "1");
		deleteRequest.routing("routing");
		deleteRequest.timeout(TimeValue.timeValueMinutes(2));
		deleteRequest.timeout("2m");
		deleteRequest.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
		deleteRequest.setRefreshPolicy("wait_for");
		deleteRequest.version(2);
		deleteRequest.versionType(VersionType.EXTERNAL);

		DeleteResponse deleteResponse = null;
		try {
			deleteResponse = esClient.delete(
					deleteRequest, RequestOptions.DEFAULT);
		} catch (Exception e) {
			ElasticsearchException exception = (ElasticsearchException) e;
			// ----------- 可能出现的异常 ------------------ \\\
			if (exception.status() == RestStatus.CONFLICT) {

			}
			e.printStackTrace();

		}

		String index = deleteResponse.getIndex();
		String id = deleteResponse.getId();
		long version = deleteResponse.getVersion();
		ReplicationResponse.ShardInfo shardInfo = deleteResponse.getShardInfo();
		if (shardInfo.getTotal() != shardInfo.getSuccessful()) {

		}
		if (shardInfo.getFailed() > 0) {
			for (ReplicationResponse.ShardInfo.Failure failure :
					shardInfo.getFailures()) {
				String reason = failure.reason();
			}
		}

		// 通过deleteRequest去判断doc是否存在, 根据返回结果来执行逻辑
		DeleteRequest request = new DeleteRequest("posts", "does_not_exist");
		DeleteResponse response = null;
		try {
			response = esClient.delete(
					request, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (response.getResult() == DocWriteResponse.Result.NOT_FOUND) {

		}
	}

	/**
	 * 更新操作
	 */
	@Test
	public void testUpdate() {
		UpdateRequest request = new UpdateRequest(INDEX_NAME, "1");
		// ------------------------------------------ 脚本方式1 -----------------------------------------------------\\
		Map<String, Object> map = new HashMap<>();
		map.put("age", "1");
		Script script = new Script(ScriptType.INLINE, "painless", "ctx._source.field += params.age", map);
		request.script(script);

		// ------------------------------------------ 脚本方式2 ------------------------------------------------------\\
		Script stored = new Script(
				ScriptType.STORED, null, "increment-field", map);
		request.script(stored);

		// ------------------------------------------ 部分修改的几种方式 ---------------------------------------------\\
		{
			String jsonString = "{" +
					"\"age\":\"1\"," +
					"\"name\":\"张三\"" +
					"}";
			request.doc(jsonString, XContentType.JSON);
		}

		{
			Map<String, Object> jsonMap = new HashMap<>();
			jsonMap.put("age", 1);
			jsonMap.put("name", "王五");
			request.doc(jsonMap);
		}

		{
			try {
				XContentBuilder builder = XContentFactory.jsonBuilder();
				builder.startObject();
				{
					builder.field("age", 1);
					builder.field("name", "王二麻子");
				}
				builder.endObject();
				request.doc(builder);

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		{
			request.doc("age", "1",
					"name", "李四");
		}

		// ------------------------------------------- 可选参数 -----------------------------------------------\\
		// routing参数
		request.routing("routing");
		// 等待主分片可用的超时时间
		request.timeout(TimeValue.timeValueSeconds(1));
		request.timeout("1s");
		// 刷新策略
		request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);
		request.setRefreshPolicy("wait_for");
		// 当存在冲突(当前更新操作在查找和索引阶段, 要修改的doc被其他update操作修改了)的时候, 重试次数
		request.retryOnConflict(3);
		// 开启source获取(等同于map的put方法存在就返回原值), 默认禁用
		request.fetchSource(true);
		// 配置source包含字段
		String[] includes = new String[]{"updated", "r*"};
		String[] excludes = Strings.EMPTY_ARRAY;
		request.fetchSource(
				new FetchSourceContext(true, includes, excludes));
		// 配置source排除字段
		String[] includesEmpty = Strings.EMPTY_ARRAY;
		String[] excludesArr = new String[]{"updated"};
		request.fetchSource(
				new FetchSourceContext(true, includesEmpty, excludesArr));
		// 文档版本号setIfSeqNo(等同于_version字段), IfPrimaryTerm(表示文档坐在位置)
		request.setIfSeqNo(2L);
		request.setIfPrimaryTerm(1L);
		// 在更新文档的时候, 没有更新任何内容的时候version不会增加, 在返回的结果中会有一个"result": "noop", detectNoop(false)
		// 就是关闭这个检查, 并且在没有任何修改的时候version也是会+1.
		request.detectNoop(false);
		// 将脚本操作设置为upsert操作(即文档存在更新, 不存在就插入一条新doc)
		request.scriptedUpsert(true);
		// 将doc操作设置为upsert操作
		request.docAsUpsert(true);
		// 设置有多少分片处于可用状态才能执行更新操作.
		request.waitForActiveShards(2);
		request.waitForActiveShards(ActiveShardCount.ALL);

		// -------------------------------------------- 执行 ------------------------------------------------------\\
		UpdateResponse updateResponse = null;
		try {
			updateResponse = esClient.update(
					request, RequestOptions.DEFAULT);
		} catch (Exception e) {
			ElasticsearchException exception = (ElasticsearchException) e;
			if (exception.status() == RestStatus.CONFLICT) {
				e.printStackTrace();
			}

		}

		// -------------------------------------------- 响应 -------------------------------------------------------\\
		String index = updateResponse.getIndex();
		String id = updateResponse.getId();
		long version = updateResponse.getVersion();
		if (updateResponse.getResult() == DocWriteResponse.Result.CREATED) {

		} else if (updateResponse.getResult() == DocWriteResponse.Result.UPDATED) {

		} else if (updateResponse.getResult() == DocWriteResponse.Result.DELETED) {

		} else if (updateResponse.getResult() == DocWriteResponse.Result.NOOP) {

		}

		GetResult result = updateResponse.getGetResult();
		if (result.isExists()) {
			String sourceAsString = result.sourceAsString();
			Map<String, Object> sourceAsMap = result.sourceAsMap();
			byte[] sourceAsBytes = result.source();
		} else {

		}

		// 可能会检查分片失败
		ReplicationResponse.ShardInfo shardInfo = updateResponse.getShardInfo();
		if (shardInfo.getTotal() != shardInfo.getSuccessful()) {

		}
		if (shardInfo.getFailed() > 0) {
			for (ReplicationResponse.ShardInfo.Failure failure :
					shardInfo.getFailures()) {
				String reason = failure.reason();
			}
		}
	}

	/**
	 *
	 */
	@Test
	public void testTermVectors() {
		TermVectorsRequest request = new TermVectorsRequest(INDEX_NAME, "1");
		request.setFields("age");
		TermVectorsResponse response = null;
		try {
			response = esClient.termvectors(request, RequestOptions.DEFAULT);
		} catch (Exception e) {
			e.printStackTrace();
		}

		String index = response.getIndex();
		String id = response.getId();
		boolean found = response.getFound();
		for (TermVectorsResponse.TermVector tv : response.getTermVectorsList()) {
			String fieldname = tv.getFieldName();
			int docCount = tv.getFieldStatistics().getDocCount();
			long sumTotalTermFreq =
					tv.getFieldStatistics().getSumTotalTermFreq();
			long sumDocFreq = tv.getFieldStatistics().getSumDocFreq();
			if (tv.getTerms() != null) {
				List<TermVectorsResponse.TermVector.Term> terms = tv.getTerms();
				for (TermVectorsResponse.TermVector.Term term : terms) {
					String termStr = term.getTerm();
					int termFreq = term.getTermFreq();
					int docFreq = term.getDocFreq();
					long totalTermFreq = term.getTotalTermFreq();
					float score = term.getScore();
					if (term.getTokens() != null) {
						List<TermVectorsResponse.TermVector.Token> tokens = term.getTokens();
						for (TermVectorsResponse.TermVector.Token token : tokens) {
							int position = token.getPosition();
							int startOffset = token.getStartOffset();
							int endOffset = token.getEndOffset();
							String payload = token.getPayload();
						}
					}
				}
			}
		}
	}

}
