## 1. Broker配置相关:

broker核心配置类: `BrokerConfig`, `NettyServerConfig`, `NettyClientConfig`, `MessageStoreConfig`

`BrokerConfig`: 主要是存储broker功能特性相关的配置信息.

`NettyServerConfig`: broker作为netty服务端, 与netty客户端producer和consumer通信的配置.详见namesrv小节

`MessageStoreConfig`: 消息数据存储配置

`NettyClientConfig`: broker作为netty客户端, 与netty服务端namesrv通信的配置.

## 2. Broker的核心 --- BrokerController:

Broker 主要负责消息的存储，投递和查询以及保证服务的高可用。Broker负责接收生产者发送的消息并存储、同时为消费者消费消息提供支持。为了实现这些功能，Broker包含几个重要的子模块：

- 通信模块：负责处理来自客户端（生产者、消费者）的请求。
- 客户端管理模块:负责管理客户端（生产者、消费者）和维护消费者的Topic订阅信息。
- 存储模块：提供存储消息和查询消息的能力，方便Broker将消息存储到硬盘。
- 高可用服务（HA Service）：提供数据冗余的能力，保证数据存储到多个服务器上，将Master Broker的数据同步到Slavew Broker上。
- 索引服务（Index service）：对投递到Broker的消息建立索引，提供快速查询消息的能力。

以上这些所有的功能都是依靠brokerConroller控制.

```java
public class BrokerController {
  	// .... 省略部分不中要log属性
    private boolean updateMasterHAServerAddrPeriodically = false;
    /**
     * broker的ip:port
     */
    private InetSocketAddress storeHost;
    /**
     * 处理pull模式消费的处理器
     */
    private final PullMessageProcessor pullMessageProcessor;
    private final MessageArrivingListener messageArrivingListener;
    private final ConsumerIdsChangeListener consumerIdsChangeListener;
    private final Broker2Client broker2Client;
    /**
     * slave保存的同步数据要使用到的消息
     */
    private final SlaveSynchronize slaveSynchronize;
    /**
     * broker的一些统计指标
     */
    private BrokerStats brokerStats;
    private BrokerFastFailure brokerFastFailure;
    /**
     * slave同步结果
     */
    private Future<?> slaveSyncFuture;
    private Map<Class,AccessValidator> accessValidatorMap = new HashMap<Class, AccessValidator>();
    // ----------------------------------- 配置属性 ------------------------------------------------- \\

    private final BrokerConfig brokerConfig;
    private final NettyServerConfig nettyServerConfig;
    private final NettyClientConfig nettyClientConfig;
    private final MessageStoreConfig messageStoreConfig;
    private Configuration configuration;
    // ----------------------------------- 功能抽象属性 ---------------------------------------------- \\
    /**
     * broker向外通信的对象
     */
    private final BrokerOuterAPI brokerOuterAPI;
    /**
     * 消息存储功能抽象属性
     */
    private MessageStore messageStore;
    /**
     * 网络通信抽象属性
     */
    private RemotingServer remotingServer;
    /**
     * 网络通信抽象属性, todo 不清楚什么东西
     */
    private RemotingServer fastRemotingServer;
    // ----------------------------------- 缓存信息 -------------------------------------------------- \\
    /**
     * 消费者 重新负载分配 缓存
     */
    private final RebalanceLockManager rebalanceLockManager = new RebalanceLockManager();
    /**
     * 消费offset管理, 缓存 concurrentMap{topic@group -> concurrentMap{queueId, offset}}
     */
    private final ConsumerOffsetManager consumerOffsetManager;
    /**
     * 消费者管理, 缓存 ConcurrentMap{Group -> ConsumerGroupInfo}, 以及channel
     */
    private final ConsumerManager consumerManager;
    /**
     * 消费过滤管理, 缓存 concurrentMap{topic -> FilterDataMapByTopic}
     */
    private final ConsumerFilterManager consumerFilterManager;
    /**
     * ConcurrentHashMap{group name -> ConcurrentHashMap{Channel -> ClientChannelInfo}}
     */
    private final ProducerManager producerManager;
    /**
     * 订阅配置信息缓存
     */
    private final SubscriptionGroupManager subscriptionGroupManager;
    private final FilterServerManager filterServerManager;
    private final BrokerStatsManager brokerStatsManager;
    private TopicConfigManager topicConfigManager;
    private final List<SendMessageHook> sendMessageHookList = new ArrayList<SendMessageHook>();
    private final List<ConsumeMessageHook> consumeMessageHookList = new ArrayList<ConsumeMessageHook>();

    // ----------------------------------- rocketmq功能线程 ------------------------------------------ \\
    private FileWatchService fileWatchService;
    /**
     * 事物消息会查功能实现类
     */
    private TransactionalMessageCheckService transactionalMessageCheckService;
    /**
     * 事物消息功能实现类
     */
    private TransactionalMessageService transactionalMessageService;
    /**
     * 事物消息会查监听器
     */
    private AbstractTransactionalMessageCheckListener transactionalMessageCheckListener;
    private final ClientHousekeepingService clientHousekeepingService;
    /**
     * 批量消息服务类, 单线程
     */
    private final PullRequestHoldService pullRequestHoldService;

    // ----------------------------------- rocketmq功能任务队列 --------------------------------------- \\
    /**
     * broker接收producer发送来消息任务的队列
     */
    private final BlockingQueue<Runnable> sendThreadPoolQueue;
    /**
     * broker接收consumer消费请求任务的队列
     */
    private final BlockingQueue<Runnable> pullThreadPoolQueue;
    private final BlockingQueue<Runnable> replyThreadPoolQueue;
    /**
     * 查询消息请求的任务队列
     */
    private final BlockingQueue<Runnable> queryThreadPoolQueue;
    /**
     * 客户端管理请求任务队列
     */
    private final BlockingQueue<Runnable> clientManagerThreadPoolQueue;
    /**
     * 处理心跳请求任务的任务队列
     */
    private final BlockingQueue<Runnable> heartbeatThreadPoolQueue;
    /**
     * 消费者管理请求的任务队列
     */
    private final BlockingQueue<Runnable> consumerManagerThreadPoolQueue;
    /**
     * commit/rollback事物消息请求的任务队列
     */
    private final BlockingQueue<Runnable> endTransactionThreadPoolQueue;

    // ----------------------------------- rocketmq功能线程池 ----------------------------------------- \\
    /**
     * 处理 发送消息请求 的线程池
     */
    private ExecutorService sendMessageExecutor;
    /**
     * 处理 消息消费请求 的线程池
     */
    private ExecutorService pullMessageExecutor;
    // 处理
    private ExecutorService replyMessageExecutor;
    /**
     * 处理 查询消息请求 的线程池
     */
    private ExecutorService queryMessageExecutor;
    /**
     * 处理 管理请求 的线程池
     */
    private ExecutorService adminBrokerExecutor;
    /**
     * 处理 客户端管理请求 的线程池
     */
    private ExecutorService clientManageExecutor;
    /**
     * 处理 心跳请求 的线程池
     */
    private ExecutorService heartbeatExecutor;
    /**
     * 处理 消费者管理请求 的线程池
     */
    private ExecutorService consumerManageExecutor;
    /**
     * 处理 commit/rollback 事务消息请求 的线程池
     */
    private ExecutorService endTransactionExecutor;
    /**
     * broker定时任务线程池子
     */
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl(
            "BrokerControllerScheduledThread"));
}
```

> 在broker的属性里面, 定义了很多的线程池, 主要是用于将业务功能与i/0功能隔离开来, 提高网络i/o的效率

## 3. Broker的启动:

### 1. 入口:

***BrokerStartup#main***

```java
public static void main(String[] args) {
    start(createBrokerController(args));
}
```

### 2. 创建BrokerController:

创建BrokerController的过程主要是做以下几个操作:

1. 根据命令行,或者加载默认的配置信息.
2. 创建broker相关的功能服务实例.
3. 初始化broker相关的功能服务.在这过程中, 部分服务会直接启动, 比如indexService等
4. 创建broker jvm关闭时的钩子函数

#### 1. 加载配置信息:

```java
    public static BrokerController createBrokerController(String[] args) {
        // 设置rocketmq通信模块的版本号
        System.setProperty(RemotingCommand.REMOTING_VERSION_KEY, Integer.toString(MQVersion.CURRENT_VERSION));
        // 设置发送socket默认缓冲区
        if (null == System.getProperty(NettySystemConfig.COM_ROCKETMQ_REMOTING_SOCKET_SNDBUF_SIZE)) {
            NettySystemConfig.socketSndbufSize = 131072;
        }
        // 设置接收socket默认缓冲区
        if (null == System.getProperty(NettySystemConfig.COM_ROCKETMQ_REMOTING_SOCKET_RCVBUF_SIZE)) {
            NettySystemConfig.socketRcvbufSize = 131072;
        }

        try {
            //PackageConflictDetect.detectFastjson();
            // 解析启动命令
            Options options = ServerUtil.buildCommandlineOptions(new Options());
            commandLine = ServerUtil.parseCmdLine("mqbroker", args, buildCommandlineOptions(options),
                new PosixParser());
            if (null == commandLine) {
                System.exit(-1);
            }
            // 加载默认配置
            final BrokerConfig brokerConfig = new BrokerConfig();
            final NettyServerConfig nettyServerConfig = new NettyServerConfig();
            final NettyClientConfig nettyClientConfig = new NettyClientConfig();

            nettyClientConfig.setUseTLS(Boolean.parseBoolean(System.getProperty(TLS_ENABLE,
                String.valueOf(TlsSystemConfig.tlsMode == TlsMode.ENFORCING))));
            // 设置默认端口
            nettyServerConfig.setListenPort(10911);
            // 加载消息存储相关配置
            final MessageStoreConfig messageStoreConfig = new MessageStoreConfig();
            // 设置salve的访问消息在内存中的比例
            if (BrokerRole.SLAVE == messageStoreConfig.getBrokerRole()) {
                int ratio = messageStoreConfig.getAccessMessageInMemoryMaxRatio() - 10;
                messageStoreConfig.setAccessMessageInMemoryMaxRatio(ratio);
            }
						// ... 解析启动参数, 覆盖默认配置相关代码省略
						// ... 2. 创建broker相关的功能服务实例  相关代码省略
          
          	// ... 3. 初始化broker相关的功能服务.在这过程中, 部分服务会直接启动, 比如indexService等 相关代码省略
          
          	// ... 4. 创建broker jvm关闭时的钩子函数 相关代码省略

            return controller;
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return null;
    }
```

首先解析broker启动命令`mqbroker`, 将其封装成对象, 然后根据broker默认的配置, 创建出broker所需要的几个配置信息实例:

1. BrokerConfig:  主要是broker实例相关的信息, 如: brokerID, BrokerName, NamesrvAddr等
2. NettyServerConfig:  Broker netty服务端配置类，Broker netty服务端主要用来接收客户端的请求，NettyServerConfig类主要属性包括监听接口、服务工作线程数、接收和发送请求的buffer大小等。
3. NettyClientConfig：netty客户端配置类，用于生产者、消费者这些客户端与Broker进行通信相关配置，配置属性主要包括客户端工作线程数、客户端回调线程数、连接超时时间、连接不活跃时间间隔、连接最大闲置时间等。
4. MessageStoreConfig：消息存储配置类，配置属性包括存储路径、commitlog文件存储目录、CommitLog文件的大小、CommitLog刷盘的时间间隔等。



接下来, broker允许通过"-c"参数, 或者通过命令里面传参来覆盖默认的参数.

```java
    public static BrokerController createBrokerController(String[] args) {
      // ... 省略部分代码      
      try {
						// ... 省略部分代码  
            if (commandLine.hasOption('c')) {
                String file = commandLine.getOptionValue('c');
                if (file != null) {
                    configFile = file;
                    InputStream in = new BufferedInputStream(new FileInputStream(file));
                    properties = new Properties();
                    properties.load(in);

                    properties2SystemEnv(properties);
                    MixAll.properties2Object(properties, brokerConfig);
                    MixAll.properties2Object(properties, nettyServerConfig);
                    MixAll.properties2Object(properties, nettyClientConfig);
                    MixAll.properties2Object(properties, messageStoreConfig);

                    BrokerPathConfigHelper.setBrokerConfigPath(file);
                    in.close();
                }
            }

            // 解析控制台普通参数
            MixAll.properties2Object(ServerUtil.commandLine2Properties(commandLine), brokerConfig);
        
						// ... 2. 创建broker相关的功能服务实例  相关代码省略
          
          	// ... 3. 初始化broker相关的功能服务.在这过程中, 部分服务会直接启动, 比如indexService等 相关代码省略
          
          	// ... 4. 创建broker jvm关闭时的钩子函数 相关代码省略

            return controller;
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }  
    }
```

下面这段代码主要是 对必须参数进行校验和填充: 比如 `rocketmqHome`, `校验并填充namesrv地址`, `强制设置masterId为0`, `校验slaveId是否大于0`.

```java
public static BrokerController createBrokerController(String[] args) {
  // ... 省略部分代码      
  try {
    // ... 省略部分代码 
    
				    if (null == brokerConfig.getRocketmqHome()) {
                System.out.printf("Please set the %s variable in your environment to match the location of the RocketMQ installation", MixAll.ROCKETMQ_HOME_ENV);
                System.exit(-2);
            }

            // 校验一下配置文件中 namesrv地址是否合法
            String namesrvAddr = brokerConfig.getNamesrvAddr();
            if (null != namesrvAddr) {
                try {
                    // 多个namesrv地址使用";"分割
                    String[] addrArray = namesrvAddr.split(";");
                    for (String addr : addrArray) {
                        RemotingUtil.string2SocketAddress(addr);
                    }
                } catch (Exception e) {
                    System.out.printf(
                        "The Name Server Address[%s] illegal, please set it as follows, \"127.0.0.1:9876;192.168.0.1:9876\"%n",
                        namesrvAddr);
                    System.exit(-3);
                }
            }

            // 强制将master broker实例id设置为0, 并校验slaveId是否>0
            switch (messageStoreConfig.getBrokerRole()) {
                case ASYNC_MASTER:
                case SYNC_MASTER:
                    brokerConfig.setBrokerId(MixAll.MASTER_ID);
                    break;
                case SLAVE:
                    if (brokerConfig.getBrokerId() <= 0) {
                        System.out.printf("Slave's brokerId must be > 0");
                        System.exit(-3);
                    }

                    break;
                default:
                    break;
            }
						// 当EnableDLegerCommitLog开启的时候, 将brokerId设置为-1
            if (messageStoreConfig.isEnableDLegerCommitLog()) {
                brokerConfig.setBrokerId(-1);
            }

            // 设置broker实例之间通信端口
            messageStoreConfig.setHaListenPort(nettyServerConfig.getListenPort() + 1);
    			
    				// 处理日志相关配置, 略过
    
					// ... 2. 创建broker相关的功能服务实例  相关代码省略
      
      	// ... 3. 初始化broker相关的功能服务.在这过程中, 部分服务会直接启动, 比如indexService等 相关代码省略
      
      	// ... 4. 创建broker jvm关闭时的钩子函数 相关代码省略

        return controller;
    } catch (Throwable e) {
        e.printStackTrace();
        System.exit(-1);
    }  
}
```

#### 2. 创建并初始化broker功能实例:

```java
public static BrokerController createBrokerController(String[] args) {
  // ... 省略部分代码      
  try {
    		// ... 省略部分代码 
    
				// 创建brokerController
        final BrokerController controller = new BrokerController(
                brokerConfig,
                nettyServerConfig,
                nettyClientConfig,
                messageStoreConfig);
      
      	// ... 3. 初始化broker相关的功能服务.在这过程中, 部分服务会直接启动, 比如indexService等 相关代码省略
      	// ... 4. 创建broker jvm关闭时的钩子函数 相关代码省略

        return controller;
    } catch (Throwable e) {
        e.printStackTrace();
        System.exit(-1);
    }  
}
```

```java
    public BrokerController(
        final BrokerConfig brokerConfig,
        final NettyServerConfig nettyServerConfig,
        final NettyClientConfig nettyClientConfig,
        final MessageStoreConfig messageStoreConfig
    ) {
        this.brokerConfig = brokerConfig;
        this.nettyServerConfig = nettyServerConfig;
        this.nettyClientConfig = nettyClientConfig;
        this.messageStoreConfig = messageStoreConfig;
        this.consumerOffsetManager = new ConsumerOffsetManager(this);
        this.topicConfigManager = new TopicConfigManager(this);
        this.pullMessageProcessor = new PullMessageProcessor(this);
        this.pullRequestHoldService = new PullRequestHoldService(this);
        this.messageArrivingListener = new NotifyMessageArrivingListener(this.pullRequestHoldService);
        this.consumerIdsChangeListener = new DefaultConsumerIdsChangeListener(this);
        this.consumerManager = new ConsumerManager(this.consumerIdsChangeListener);
        this.consumerFilterManager = new ConsumerFilterManager(this);
        this.producerManager = new ProducerManager();
        this.clientHousekeepingService = new ClientHousekeepingService(this);
        this.broker2Client = new Broker2Client(this);
        this.subscriptionGroupManager = new SubscriptionGroupManager(this);
        this.brokerOuterAPI = new BrokerOuterAPI(nettyClientConfig);
        this.filterServerManager = new FilterServerManager(this);

        this.slaveSynchronize = new SlaveSynchronize(this);

        this.sendThreadPoolQueue = new LinkedBlockingQueue<Runnable>(this.brokerConfig.getSendThreadPoolQueueCapacity());
        this.pullThreadPoolQueue = new LinkedBlockingQueue<Runnable>(this.brokerConfig.getPullThreadPoolQueueCapacity());
        this.replyThreadPoolQueue = new LinkedBlockingQueue<Runnable>(this.brokerConfig.getReplyThreadPoolQueueCapacity());
        this.queryThreadPoolQueue = new LinkedBlockingQueue<Runnable>(this.brokerConfig.getQueryThreadPoolQueueCapacity());
        this.clientManagerThreadPoolQueue = new LinkedBlockingQueue<Runnable>(this.brokerConfig.getClientManagerThreadPoolQueueCapacity());
        this.consumerManagerThreadPoolQueue = new LinkedBlockingQueue<Runnable>(this.brokerConfig.getConsumerManagerThreadPoolQueueCapacity());
        this.heartbeatThreadPoolQueue = new LinkedBlockingQueue<Runnable>(this.brokerConfig.getHeartbeatThreadPoolQueueCapacity());
        this.endTransactionThreadPoolQueue = new LinkedBlockingQueue<Runnable>(this.brokerConfig.getEndTransactionPoolQueueCapacity());

        this.brokerStatsManager = new BrokerStatsManager(this.brokerConfig.getBrokerClusterName());
        this.setStoreHost(new InetSocketAddress(this.getBrokerConfig().getBrokerIP1(), this.getNettyServerConfig().getListenPort()));

        this.brokerFastFailure = new BrokerFastFailure(this);
        this.configuration = new Configuration(
            log,
            BrokerPathConfigHelper.getBrokerConfigPath(),
            this.brokerConfig, this.nettyServerConfig, this.nettyClientConfig, this.messageStoreConfig
        );
    }
```

#### 3. 初始化broker功能实例:

```java
public static BrokerController createBrokerController(String[] args) {
  // ... 省略部分代码      
  try {
    		// ... 省略部分代码
      	// ... 2. 创建broker相关的功能服务实例  相关代码省略
      	// ... 3. 初始化broker相关的功能服务.在这过程中, 部分服务会直接启动, 比如indexService等 相关代码省略
            boolean initResult = controller.initialize();
            if (!initResult) {
                controller.shutdown();
                System.exit(-3);
            }
      	// ... 4. 创建broker jvm关闭时的钩子函数 相关代码省略

        return controller;
    } catch (Throwable e) {
        e.printStackTrace();
        System.exit(-1);
    }  
}
```

在初始化broker功能实例的时候主要是做以下几件事情:

1. 加载topic, consumeOffset, subscriptionGroup, consumerFileter等信息.
2. 加载commitlog数据
3. 创建netty通信服务端实例
4. 初始化业务线程池 
5. 注册opaque 与 pair<线程池,处理器>的映射关系
6. 创建broker 定时任务
7. 处理ssl相关功能(如果开启)
8. 初始化broker事物消息组件
9. 初始化acl组件
10. 注册RpcHook

***BrokerController#initialize***: 下面的代码段都是在这个方法里面的

```java
    public boolean initialize() throws CloneNotSupportedException {
				// 1. 加载topic, consumeOffset, subscriptionGroup, consumerFileter等信息.
				// 2. 加载commitLog数据
        // 3. 创建netty通信服务端实例
        // 4. 初始化业务线程池
        // 5. 创建broker 定时任务
        // 6. 初始化broker 事物消息组件
        // 7. 初始化acl组件
        // 8. 注册RpcHook
    }
```

##### 1.加载持久化数据:

```java
        // 加载topic.json 或者 topic.json.bak
        boolean result = this.topicConfigManager.load();
        // 加载consumerOffset.json 或者 consumerOffset.json.bak
        result = result && this.consumerOffsetManager.load();
        // 加载 subscriptionGroup.json 或者 subscriptionGroup.json.bak
        result = result && this.subscriptionGroupManager.load();
        // 加载 consumerFilter.json 或者 consumerFilter.json.bak
        result = result && this.consumerFilterManager.load();
```

这里主要是加载broker在运行期间 持久化的topic, consumeOffset, subscriptionGroup, consumerFileter 等数据. 路径为: `${user.home}/store/config/`.

> 某些操作都需要加载成功才能继续做下面操作的时候, 就可以使用上面这个段代码的写法.

##### 2. 加载commitLog文件:

```java
        if (result) {
            try {
                // 创建 消息存储实现对象
                this.messageStore =
                    new DefaultMessageStore(this.messageStoreConfig, this.brokerStatsManager, this.messageArrivingListener,
                        this.brokerConfig);
                if (messageStoreConfig.isEnableDLegerCommitLog()) {
                    DLedgerRoleChangeHandler roleChangeHandler = new DLedgerRoleChangeHandler(this, (DefaultMessageStore) messageStore);
                    ((DLedgerCommitLog)((DefaultMessageStore) messageStore).getCommitLog()).getdLedgerServer().getdLedgerLeaderElector().addRoleChangeHandler(roleChangeHandler);
                }
                this.brokerStats = new BrokerStats((DefaultMessageStore) this.messageStore);
                //load plugin
                MessageStorePluginContext context = new MessageStorePluginContext(messageStoreConfig, brokerStatsManager, messageArrivingListener, brokerConfig);
                this.messageStore = MessageStoreFactory.build(context, this.messageStore);
                this.messageStore.getDispatcherList().addFirst(new CommitLogDispatcherCalcBitMap(this.brokerConfig, this.consumerFilterManager));
            } catch (IOException e) {
                result = false;
                log.error("Failed to initialize", e);
            }
        }
        // 加载commitlog文件
        result = result && this.messageStore.load();
```

在rocketmq功能相关的持久化记录数据都加载成功后, 就开始创建 `消息存储实现对象`或者`多副本消息存储对象`, `broker状态统计组件`, `消息插件`, 然后加载commitLog文件.

##### 3. 创建netty通信实例:

```java
        if (result) {
            // 创建netty服务端
            this.remotingServer = new NettyRemotingServer(this.nettyServerConfig, this.clientHousekeepingService);
            NettyServerConfig fastConfig = (NettyServerConfig) this.nettyServerConfig.clone();
            // fastremotingServer 监听10909
            fastConfig.setListenPort(nettyServerConfig.getListenPort() - 2);
            this.fastRemotingServer = new NettyRemotingServer(fastConfig, this.clientHousekeepingService);
          
            // 省略代码.... 
        }
```

创建netty服务器的时候创建了两个，一个是普通的，一个是快速的，remotingServer用来与生产者、消费者进行通信。当isSendMessageWithVIPChannel=true的时候会选择port-2的fastRemotingServer进行的消息的处理，为了防止某些很重要的业务阻塞，就再开启了一个remotingServer进行处理，但是现在默认是不开启的，fastRemotingServer主要是为了兼容老版本的RocketMQ。

##### 4. 初始化业务功能线程池:

```java
        if (result) {
						// 省略代码 ....
                this.sendMessageExecutor = new BrokerFixedThreadPoolExecutor(
                this.brokerConfig.getSendMessageThreadPoolNums(),
                this.brokerConfig.getSendMessageThreadPoolNums(),
                1000 * 60,
                TimeUnit.MILLISECONDS,
                this.sendThreadPoolQueue,
                new ThreadFactoryImpl("SendMessageThread_"));   
          	// pullRequest业务线程池
          	// replyRequest业务线程池
            // queryRequest业务线程池
          	// adminRequest业务线程池
          	// clientManageRequest业务线程池
          	// heatBeatRequest业务线程池
          	// endTransactionRequest业务线程池
          	// 消费者管理线程池
          
            // 省略代码 .... 
        }
```

> 上面只是以 sendMessageExecutor为例, 还有好几个线程池, 基本上不同的功能都创建了一个线程池来执行任务

##### 5. 注册opaque 与 pair<线程池,处理器>的映射关系:

这一步的主要作用是 `将业务请求, 通过约定好的opaque(请求ID)进行分发, 不同业务功能使用不同的线程池来处理, 将网络I/O和业务功能隔离开来, 提高效率`

```java
// BrokerController#initialize
this.registerProcessor();

// BrokerController#registerProcessor()
public void registerProcessor() {
        // opaque -> Pair<SendMessageProcessor,sendMessageExecutor>
        
        /**
         * 创建处理producer发送过来消息的处理器,钩子函数,消费消息的钩子函数
         */
        SendMessageProcessor sendProcessor = new SendMessageProcessor(this);
        sendProcessor.registerSendMessageHook(sendMessageHookList);
        sendProcessor.registerConsumeMessageHook(consumeMessageHookList);

        this.remotingServer.registerProcessor(RequestCode.SEND_MESSAGE, sendProcessor, this.sendMessageExecutor);
        this.remotingServer.registerProcessor(RequestCode.SEND_MESSAGE_V2, sendProcessor, this.sendMessageExecutor);
        this.remotingServer.registerProcessor(RequestCode.SEND_BATCH_MESSAGE, sendProcessor, this.sendMessageExecutor);
        this.remotingServer.registerProcessor(RequestCode.CONSUMER_SEND_MSG_BACK, sendProcessor, this.sendMessageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.SEND_MESSAGE, sendProcessor, this.sendMessageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.SEND_MESSAGE_V2, sendProcessor, this.sendMessageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.SEND_BATCH_MESSAGE, sendProcessor, this.sendMessageExecutor);
        this.fastRemotingServer.registerProcessor(RequestCode.CONSUMER_SEND_MSG_BACK, sendProcessor, this.sendMessageExecutor);
      // opaque -> Pair<ReplyMessageProcessor,replyMessageExecutor>
      // opaque -> Pair<QueryMessageProcessor,queryMessageExecutor>
      // opaque -> Pair<ClientManageProcessor,heartbeatExecutor>
      // opaque -> Pair<ConsumerManageProcessor,consumerManageExecutor>
      // opaque -> Pair<EndTransactionProcessor,endTransactionExecutor>
      // opaque -> Pair<AdminBrokerProcessor,adminBrokerExecutor>
    }
```

这个实际上很简单, romotingServer对象持有一个 hashmap{ requestCode -> Pair<processor, executors>} 的属性, 这里的注册就是将对应的映射关系存入这个hashMap属性里面去. 

##### 6. 创建broker的定时任务:

broker初始化的时候会创建一些定时任务, 全部又brokerController的 scheduledExecutorService(`单线程, 减少cpu的竞争`)属性持有的定时任务线程池执行. `都是调用scheduleAtFixedRate以固定频率执行`

- 在info.log打印 当天生产,消费消息条数, 24小时执行一次

- 持久化消费offset到 `consumerOffset.json`文件, 每5秒执行一次

- 持久化消费过滤信息到 `consumerFilter.json`, 每10秒执行一次

- broker保护检测, 当disableConsumeIfConsumerReadSlowly为true的时候, 就会去检查broker的消费者组是否存在消费的很慢的组, 有就停止其消费, 防止消息积压.每3分钟执行一次

  ```java
  this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
      @Override
      public void run() {
         try {
              BrokerController.this.protectBroker();
         } catch (Throwable e) {
              log.error("protectBroker error.", e);
         }
      }
  }, 3, 3, TimeUnit.MINUTES);
  ```

  **BrokerController#protectBroker()**: 逻辑就是 当disableConsumeIfConsumerReadSlowly为true, 消费者组的消费进度落后的值 大于 消费落后阈值时就禁止消费者组消费 

  ```java
      public void protectBroker() {
          if (this.brokerConfig.isDisableConsumeIfConsumerReadSlowly()) {
              final Iterator<Map.Entry<String, MomentStatsItem>> it = this.brokerStatsManager.getMomentStatsItemSetFallSize().getStatsItemTable().entrySet().iterator();
              while (it.hasNext()) {
                  final Map.Entry<String, MomentStatsItem> next = it.next();
                  final long fallBehindBytes = next.getValue().getValue().get();
                  if (fallBehindBytes > this.brokerConfig.getConsumerFallbehindThreshold()) {
                      final String[] split = next.getValue().getStatsKey().split("@");
                      final String group = split[2];
                      LOG_PROTECTION.info("[PROTECT_BROKER] the consumer[{}] consume slowly, {} bytes, disable it", group, fallBehindBytes);
                      this.subscriptionGroupManager.disableConsume(group);
                  }
              }
          }
      }
  ```

- 打印队列的大小以及队列头部元素存在的时间定时任务, 每秒执行一次:

  会打印发送消息线程池队列、拉取消息线程池队列、查询消息线程池队列、结束事务线程池队列的大小，以及打印队列头部元素存在的时间，这个时间等于当前时间减去头部元素创建的时间，就是该元素创建到现在已经花费了多长时间。

- 打印已存储在commitlog中但尚未记录到comsumeQueue中的字节数, 每分钟执行一次.

- 如果配置了namesrv的地址就记录namesrv的地址, 如果FetchNamesrvAddrByAddressServer为true, 就创建定时任务, 从地址服务器上面去获取namesrv的地址, 路径是设置好了的, 每2分钟执行一次

- 如果 `EnableDLegerCommitLog == false` 即没有开启多副本存储机制

  slave: 更新配置里面的HaMasterAddress到 messageStore服务对象里面去, 同步数据时使用

  master: 创建一个定时任务, 定时打印slave数据落后master多少, 每分钟执行一次

##### 7. 处理ssl相关功能:

> todo

##### 8. 初始化事物消息组件:

***BrokerController#initialTransaction()***: 主要是创建 事物消息服务实现类实例, 事务消息回查监听器实例, 事务消息回查检查服务实例

```java
    private void initialTransaction() {
        // 通过spring的spi加载TransactionalMessageService(4.9.1好像没有这个了)
        this.transactionalMessageService = ServiceProvider.loadClass(ServiceProvider.TRANSACTION_SERVICE_ID, TransactionalMessageService.class);
        if (null == this.transactionalMessageService) {
            this.transactionalMessageService = new TransactionalMessageServiceImpl(new TransactionalMessageBridge(this, this.getMessageStore()));
            log.warn("Load default transaction message hook service: {}", TransactionalMessageServiceImpl.class.getSimpleName());
        }
        // 通过spring的spi加载transactionalMessageCheckListener(4.9.1好像没有这个了)
        this.transactionalMessageCheckListener = ServiceProvider.loadClass(ServiceProvider.TRANSACTION_LISTENER_ID, AbstractTransactionalMessageCheckListener.class);
        if (null == this.transactionalMessageCheckListener) {
            this.transactionalMessageCheckListener = new DefaultTransactionalMessageCheckListener();
            log.warn("Load default discard message hook service: {}", DefaultTransactionalMessageCheckListener.class.getSimpleName());
        }
        this.transactionalMessageCheckListener.setBrokerController(this);
        // 初始化事务消息回差服务
        this.transactionalMessageCheckService = new TransactionalMessageCheckService(this);
    }
```

- transactionalMessageCheckService用于检查超时的 Half 消息是否需要回查.

##### 9. 初始化acl权限控制组件:

initialAcl方法主要是加载权限相关校验器，RocketMQ的相关的管理的权限验证和安全就交给这里的加载的校验器了。

```java
private void initialAcl() {
        if (!this.brokerConfig.isAclEnable()) {
            log.info("The broker dose not enable acl");
            return;
        }
        //利用SPI加载权限相关的校验器
        List<AccessValidator> accessValidators = ServiceProvider.load(ServiceProvider.ACL_VALIDATOR_ID, AccessValidator.class);
        if (accessValidators == null || accessValidators.isEmpty()) {
            log.info("The broker dose not load the AccessValidator");
            return;
        }
        //利用SPI加载权限相关的校验器
        for (AccessValidator accessValidator: accessValidators) {
            final AccessValidator validator = accessValidator;
            accessValidatorMap.put(validator.getClass(),validator);
            this.registerServerRPCHook(new RPCHook() {

                @Override
                public void doBeforeRequest(String remoteAddr, RemotingCommand request) {
                    //Do not catch the exception
                    validator.validate(validator.parse(request, remoteAddr));
                }

                @Override
                public void doAfterResponse(String remoteAddr, RemotingCommand request, RemotingCommand response) {
                }
            });
        }
    }
```

##### 10. 注册钩子函数:

```java
private void initialRpcHooks() {

      //利用SPI加载钩子
      List<RPCHook> rpcHooks = ServiceProvider.load(ServiceProvider.RPC_HOOK_ID, RPCHook.class);
      if (rpcHooks == null || rpcHooks.isEmpty()) {
           return;
       }
        //注册钩子
       for (RPCHook rpcHook: rpcHooks) {
           this.registerServerRPCHook(rpcHook);
       }
}
```

#### 4. 添加jvm关闭钩子函数:

```java
            // jvm退出钩子函数,
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                private volatile boolean hasShutdown = false;
                private AtomicInteger shutdownTimes = new AtomicInteger(0);

                @Override
                public void run() {
                    synchronized (this) {
                        log.info("Shutdown hook was invoked, {}", this.shutdownTimes.incrementAndGet());
                        if (!this.hasShutdown) {
                            this.hasShutdown = true;
                            long beginTime = System.currentTimeMillis();
                            controller.shutdown();
                            long consumingTimeTotal = System.currentTimeMillis() - beginTime;
                            log.info("Shutdown hook over, consuming total time(ms): {}", consumingTimeTotal);
                        }
                    }
                }
            }, "ShutdownHook"));
```

> 加锁的原因是: shutDownHock是多线程执行的, 可能造成线程安全问题

### 3. 启动:

***org.apache.rocketmq.broker.BrokerStartup#start***:

```java

public static BrokerController start(BrokerController controller) {
        try {

            //Broker控制器启动
            controller.start();

            //打印Broker成功的消息
            String tip = "The broker[" + controller.getBrokerConfig().getBrokerName() + ", "
                + controller.getBrokerAddr() + "] boot success. serializeType=" + RemotingCommand.getSerializeTypeConfigInThisServer();

            if (null != controller.getBrokerConfig().getNamesrvAddr()) {
                tip += " and name server is " + controller.getBrokerConfig().getNamesrvAddr();
            }

            log.info(tip);
            System.out.printf("%s%n", tip);
            return controller;
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }

        return null;
}
```

***BrokerController#start()***

```java
    public void start() throws Exception {
        if (this.messageStore != null) {
            // 启动存储服务
            this.messageStore.start();
        }

        if (this.remotingServer != null) {
            // nettyServer
            this.remotingServer.start();
        }

        if (this.fastRemotingServer != null) {
            // 兼容老版本代码的nettyServer
            this.fastRemotingServer.start();
        }

        if (this.fileWatchService != null) {
            // 文件变动监听服务
            this.fileWatchService.start();
        }

        if (this.brokerOuterAPI != null) {
            // broker对外外通信api
            this.brokerOuterAPI.start();
        }

        if (this.pullRequestHoldService != null) {
            // 拉取消息服务
            this.pullRequestHoldService.start();
        }

        if (this.clientHousekeepingService != null) {
            // 客户端长连接轮训服务
            this.clientHousekeepingService.start();
        }

        if (this.filterServerManager != null) {
            // 消息过滤管理服务
            this.filterServerManager.start();
        }

        // 没有开启多副本机制
        if (!messageStoreConfig.isEnableDLegerCommitLog()) {
            // master会启动事物消息回查服务
            startProcessorByHa(messageStoreConfig.getBrokerRole());
            // 主从同步, master将ha地址设置为null, slave开启定时任务, 每10秒钟同步一次
            handleSlaveSynchronize(messageStoreConfig.getBrokerRole());
            this.registerBrokerAll(true, false, true);
        }
        // 定时向namesrv注册broker
        this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    BrokerController.this.registerBrokerAll(true, false, brokerConfig.isForceRegister());
                } catch (Throwable e) {
                    log.error("registerBrokerAll Exception", e);
                }
            }
        }, 1000 * 10, Math.max(10000, Math.min(brokerConfig.getRegisterNameServerPeriod(), 60000)), TimeUnit.MILLISECONDS);
        // broker状态管理服务
        if (this.brokerStatsManager != null) {
            this.brokerStatsManager.start();
        }
        // broker快速失败服务
        if (this.brokerFastFailure != null) {
            this.brokerFastFailure.start();
        }


    }
```



## 20. 总结:

### 1. 如何实现消息读取的高性能:

- commitLog文件是顺序写入的

- 无论是commitlog文件, consumequeue文件, 还是indexFile文件都是采取的定长设计, commitlog默认1G, consumequeue默认是20字节*30w, 定长的设计对于mmap技术的实现效率很高
- 消息发送的时候采用异步刷盘, 将消息存入内存中就直接确认消息已经存入磁盘, 降低等待消息写入的时间, 从而降低消息发送在broker端的延迟

