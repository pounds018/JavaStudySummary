# 3.0 依赖以及初始化客户端:

```xml
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-high-level-client</artifactId>
    <version>7.17.1</version>
</dependency>
```

- 关于客户端的兼容性:

``RestHighLevelClient`可以支持与更高版本的es节点进行通信, 比如: 上面依赖的客户端为7.17.1, 可以支持7.17.1以及以上版本的es, `与7.17.0以及以下版本的es通信时可能会出现不兼容的情况, 因为客户端更新的话请求参数或者api是对应高版本es的, 低版本es可能没有这些api或者参数`

- 初始化客户端:

  ```java
  @Data
  @Configuration
  @ConfigurationProperties(prefix = "elasticsearch")
  public class EsClientConfig {
      private String host;
      private Integer port1;
      private Integer port2;
      private Integer port3;
  
      @Bean
      public RestHighLevelClient esClient(){
          return new RestHighLevelClient(
              RestClient.builder(
                      new HttpHost(host,port1,"http"),
                      new HttpHost(host,port2,"http"),
                      new HttpHost(host,port3,"http")
              )
          );
      }
  }
  ```

  yaml配置:

  ```yaml
  elasticsearch:
    host: 192.168.200.130
    port1: 9201
    port2: 9202
    port3: 9203
  ```

  `RestHighLevelClient`内部会创建一个用于执行请求的`LowLevelRestClient`.`LowLevelRestClient`维护着连接池, 所以在请求处理完之后要及时`关闭连接 [client.close()]`, 以便于释放资源

# 3.1 请求选项:

下面演示的都是`LowLevelRestClient`, 初始化`LowLevelClient(就是RestClient)`

```java
	@Before
	public void init() throws Exception {
		lowLevelClient = RestClient.builder(
				new HttpHost("192.168.200.130", 9201, "http"),
				new HttpHost("192.168.200.130", 9202, "http"),
				new HttpHost("192.168.200.130", 9203, "http")
		).build();
	}
```

`RestHighLevelClient`中的所有`api`都接受`RequestOptions`，可以使用它来定制请求，而不会改变`Elasticsearch`执行请求结果。  

- 客户端可以以 `异步performRequestAysnc` 或者 `同步performanceRequest` 的方式执行请求:

  ```java
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
  ```

- 添加请求头和请求体:

  ```java
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
  ```

- 共享请求选项:

  ```java
  	private static final RequestOptions COMMON_OPTIONS;
  	static {
  		RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder()
  				// 添加全局共享header, 所有请求都会带上这部分header
  				.addHeader("Authorization", "Bearer ");
  		builder.setHttpAsyncResponseConsumerFactory(
  			new HttpAsyncResponseConsumerFactory.HeapBufferedResponseConsumerFactory(30 * 1024 * 1024 * 1024));
  
  		COMMON_OPTIONS = builder.build();
  	}
  
  	/**
  	 * 共享请求头数据
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
  ```

  # 3.1 文档api

## 1. 操作单个文档: