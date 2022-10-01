尝试搭建以下elk日志系统,为日后学习做准备,es为3结点集群
## 1.创建文件:
![image-20211002164131849](es集群搭建/image-20211002164131849.png)

## 2. 编写配置文件:

```yml
cluster.name: es-test-cluster
node.name: es-node1
network.bind_host: 0.0.0.0
network.publish_host: 172.21.0.11
http.port: 9201
transport.tcp.port: 9301
http.cors.enabled: true
http.cors.allow-origin: "*"
node.master: true
node.data: true
discovery.zen.ping.unicast.hosts: ["172.21.0.11:9301","172.21.0.12:9302","172.21.0.13:9303"]
cluster.initial_master_nodes: ["es-node1", "es-node2","es-node3"]
discovery.zen.minimum_master_nodes: 2
bootstrap.memory_lock: true
path.data: /es/data
path.logs: /es/logs
```

```yml
cluster.name: es-test-cluster
node.name: es-node2
network.bind_host: 0.0.0.0
network.publish_host: 172.21.0.12
http.port: 9202
transport.tcp.port: 9302
http.cors.enabled: true
http.cors.allow-origin: "*"
node.master: true
node.data: true
discovery.zen.ping.unicast.hosts: ["172.21.0.11:9301","172.21.0.12:9302","172.21.0.13:9303"]
cluster.initial_master_nodes: ["es-node1", "es-node2","es-node3"]
discovery.zen.minimum_master_nodes: 2
bootstrap.memory_lock: true
path.data: /es/data
path.logs: /es/logs
```

```yml
cluster.name: es-test-cluster
node.name: es-node3
network.bind_host: 0.0.0.0
network.publish_host: 172.21.0.13
http.port: 9203
transport.tcp.port: 9303
http.cors.enabled: true
http.cors.allow-origin: "*"
node.master: true
node.data: true
discovery.zen.ping.unicast.hosts: ["172.21.0.11:9301","172.21.0.12:9302","172.21.0.13:9303"]
cluster.initial_master_nodes: ["es-node1", "es-node2","es-node3"]
discovery.zen.minimum_master_nodes: 2
bootstrap.memory_lock: true
path.data: /es/data
path.logs: /es/logs
```

```yml
server.name: kibana
server.host: "0"
elasticsearch.hosts: [ "http://es-node1:9201", "http://es-node2:9202", "http://es-node3:9203" ]
xpack.monitoring.ui.container.elasticsearch.enabled: false
# 语言变为中文
i18n.locale: "zh-CN"
```

## 3. compose配置:

```yml
version: "3.5"
services:
  es-node1:
    image: elasticsearch:7.14.1
    hostname: es-node1
    container_name: es-node1
    restart: always
    expose:
      - "9001"
    ports:
      - "9201:9201"
      - "9301:9301"
    volumes:
      - ./node1/conf/nodes-9201.yml:/usr/share/elasticsearch/config/elasticsearch.yml
      - ./node1/data:/es/data
      - ./node1/logs:/es/logs
      - ./node1/plugins:/usr/share/elasticsearch/plugins
    environment:
      - TZ=Asia/Shanghai
      - cluster.name=es-cluster
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms256m -Xmx256m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    networks:
      es-cluster-network:
        ipv4_address: 172.21.0.11
  es-node2:
    image: elasticsearch:7.14.1
    hostname: es-node2
    container_name: es-node2
    restart: always
    expose:
      - "9002"
    ports:
      - "9202:9202"
      - "9302:9302"
    volumes:
      - ./node2/conf/nodes-9202.yml:/usr/share/elasticsearch/config/elasticsearch.yml
      - ./node2/data:/es/data
      - ./node2/logs:/es/logs
      - ./node2/plugins:/usr/share/elasticsearch/plugins
    environment:
      - TZ=Asia/Shanghai
      - cluster.name=es-cluster
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms256m -Xmx256m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    networks:
      es-cluster-network:
        ipv4_address: 172.21.0.12
  es-node3:
    image: elasticsearch:7.14.1
    hostname: es-node3
    container_name: es-node3
    restart: always
    expose:
      - "9003"
    ports:
      - "9203:9203"
      - "9303:9303"
    volumes:
      - ./node3/conf/nodes-9203.yml:/usr/share/elasticsearch/config/elasticsearch.yml
      - ./node3/data:/es/data
      - ./node3/logs:/es/logs
      - ./node3/plugins:/usr/share/elasticsearch/plugins
    environment:
      - TZ=Asia/Shanghai
      - cluster.name=es-cluster
      - bootstrap.memory_lock=true
      - "ES_JAVA_OPTS=-Xms256m -Xmx256m"
    ulimits:
      memlock:
        soft: -1
        hard: -1
    networks:
      es-cluster-network:
        ipv4_address: 172.21.0.13  
  kibana:
    image: kibana:7.14.1
    container_name: kibana
    restart: always
    ports:
      - "5601:5601"
    volumes:
      - ./kibana.yml:/usr/share/kibana/config/kibana.yml
    environment:
      - ELASTICSEARCH_URL=http://es-node1:9201
    networks:
      - es-cluster-network
    depends_on:
      - es-node1
      - es-node2
      - es-node3
  # 暂时不搞这个(2021-10.4)    
  # logstash:
  #   image: logstash:7.14.1
  #   container_name: logstash
  #   networks:
  #     - es-cluster-network
  #   environment:
  #     - TZ=Asia/Shanghai
  #     - "LS_JAVA_OPTS=-Xms256m -Xmx256m"
  #   depends_on:
  #     - es-node1
  #     - es-node2
  #     - es-node3
networks:
  es-cluster-network:
    name: es-cluster-network
    driver: bridge
    ipam:
      driver: default
      config:
      - subnet: 172.21.0.0/16
        gateway: 172.21.0.1
```

注意事项:

1. `networkes`: 在配置网络的时候, 应该尽量加上`name属性`, 不然容易造成容器之间网络不通

2. 如果容器挂载的时候出现权限问题, 这里是宿主机除了问题, 不是容器的问题,在宿主机中加上读写权限.如: `chmod -R 777 文件夹`

3. docker退出代码 137是宿主机内存不足, 需要给宿主机扩容.

4. 启动是时候可能出现:

   ```java
   [1]: max virtual memory areas vm.max_map_count [65530] is too low, increase to at least [262144]（elasticsearch用户拥有的内存权限太小，至少需要262144）
   ```

   处理办法:

   ```bash
   # 修改配置sysctl.conf
   [root@localhost ~]# vi /etc/sysctl.conf
   # 添加下面配置：
   vm.max_map_count=262144
   # 重新加载：
   [root@localhost ~]# sysctl -p
   # 最后重新启动elasticsearch，即可启动成功。
   ```

5. 安装插件:

   以安装ik分词器为例子: 

   1. docker-compose中文件挂载 `- ./node2/plugins:/usr/share/elasticsearch/plugins`, 这一句后面的是容器镜像默认的插件存放的文件夹, 即`/usr/share/elasticsearch/plugins`. 

      这个默认插件路径可以通过`path.plugins`配置项在es.yaml配置文件中设置

   2. 将需要安装的插件放在上面挂载的文件夹中, `注意要是编译后的插件包, 而不是source包`

   3. 一定要重启es实例!一定要重启es实例!一定要重启es实例!