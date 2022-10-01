package cn.pounds;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author: pounds
 * @date: 2022/3/7 22:10
 * @desc:
 */
@SpringBootTest
public class RequestOptionsTest {
	@Resource
	private RestHighLevelClient esClient;
	private RestClient lowLevelClient;

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

	@Before
	public void init() throws Exception {
		lowLevelClient = RestClient.builder(
				new HttpHost("192.168.200.130", 9201, "http"),
				new HttpHost("192.168.200.130", 9202, "http"),
				new HttpHost("192.168.200.130", 9203, "http")
		).build();
	}

	/**
	 * 同步和异步执行请求演示
	 */
	@Test
	public void test(){
		try {
			Request request = new Request("GET", "/");
			// 同步
			Response response = lowLevelClient.performRequest(request);
			System.out.println("------------------- sync ------------------");
			System.out.println(response);
			// 输出结果: Response{requestLine=GET / HTTP/1.1, host=http://192.168.200.130:9201, response=HTTP/1.1 200 OK}

			// 异步
			Cancellable cancellable = lowLevelClient.performRequestAsync(request, new ResponseListener() {
				@Override
				public void onSuccess(Response response) {
					System.out.println("------------------- onSuccess ------------------");
					System.out.println(response);
				}

				@Override
				public void onFailure(Exception e) {
					System.out.println("------------------- onSuccess ------------------");
					e.printStackTrace();
				}
			});

		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 添加请求体和request参数
	 */
	@Test
	public void test2(){
		Request request = new Request("GET", "/megacorp");
		// 设置参数
		request.addParameter("pretty", "true");

		// 请求体, 方式1
		request.setEntity(new NStringEntity(
				"{\"json\":\"text\"}",
				ContentType.APPLICATION_JSON));
		// 请求体, 方式2
		request.setJsonEntity("{\"json\":\"text\"}");
	}

	/**
	 * 共享请求数据
	 */
	@Test
	public void test3(){
		Request request = new Request("GET", "/megacorp");

		// 在发送请求的时候setOptions
		request.setOptions(COMMON_OPTIONS);
		RequestOptions.Builder options = COMMON_OPTIONS.toBuilder();
		// 定义当前请求私有header
		options.addHeader("cats", "knock things off of other things");
		request.setOptions(options);
	}
}
