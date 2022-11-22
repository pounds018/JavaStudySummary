## 9. 数据库：

### 9.7 AOF、RDB和复制功能对过期键的处理:

#### 1. 生成RDB文件：

在执行SAVE命令文件或者BGSAVE命令文件创建一个新的RDH文件时， 程序会对数据库中的键进行检查， `已经过期的键是不会被保存到新创建的RDB文件`中。

`举个例子：`

当前数据库中存在key1， key2， key3， key1过期了， 在生成RDB文件的时候， 只会讲key2， key3保存进RDB文件中。

#### 2. 载入RDB文件:

在启动Redis服务器时, 如果服务器开启了RDB功能, 那么服务器将对RDB文件进行载入:

- 如果服务器以`master`模式运行, 那么载入RDB文件时, 程序会对文件中保存的键进行检查, `未过期的键会被载入到数据库中, 而过期键则会被忽略`.
- 如果服务器以`slave`模式运行, 那么载入RDB文件时, 文件中`保存的所有键,不论是否过期, 都会被载入到数据库中`. 不过, 因为主从服务器在进行数据同步的时候, 从服务器的数据库就会被清空. 

#### 3. AOF写入:

当服务器以AOF持久化模式运行时, 如果数据库中的某个键已经过期, 但它还没有被惰性删除或者定期删除, 因为AOF文件记录的时修改操作命令, 所以此时生成AOF文件不会对过期的键产生任何的影响. `当过期键被惰性删除或者定期删除之后, 程序会向AOF文件追加一条DEL命令, 来显示的记录该键已被删除`.

`举个例子:`

当客户端试用 `GET message`命令, 视图访问过期的message, 那么服务器将执行下面三个动作:

1. 从数据库删除message键
2. 追加一条`DEL message`命令到AOF文件
3. 向执行GET命令的客户端返回空回复

#### 4. AOF重写:

在执行AOF重写的过程中, `程序会对数据库中的键进行检查, 已经过期的键不会被保存到重写后的文件中`.

`举个例子:`

如果数据库中包含三个键 `key1,key2,key3`, 且`key2过期了`. 那么在重写工作时, 程序只会对key1, key3进行重写, key2会被忽略.

#### 5. 复制:

当服务器运行在复制模式下, `从服务器的过期键删除动作由主服务器控制`:

- `主服务器`在删除过期键之后, 会显示的向从服务器发送一个`DEL命令`, 告知从服务器删除这个过期键.
- `从服务器`在执行客户端发送读命令时, 即使碰到过期键也不会将过期键删除, 而时继续`像处理未过期键一样处理`过期键.
- `从服务器`只有在接收到`主服务器`发来的DEL命令之后, 才会删除过期键.

> todo 需要梳理下主从模式下, 读写操作是如果分配的, 这里不太理解slave接到读命令之后, 仍然会被给客户端返回数据, 这里是不是有数据不一致的情况.



### 9.8 数据库通知:

数据库通知是Redis 2.8版本新增加的功能, 这个功能可以让客户端通过订阅给定的频道或者模式, 来获取数据库中键的变化, 以及数据库中命令的执行情况.

- `键空间通知:` 关注数据库中某个键执行了什么命令.

  `SUBSCRIBE __keyspace@数据库编号:键名称`

  ```shell
  127.0.0.1:6379>SUBSCRIBE __keyspace@0__:message
  Reading messages...(press Ctrl-C to quit)
  1) "subscribe"
  2) "__keyspace@0__:message"
  3) (integer) 1
  
  1) "message"
  2) "__keyspace@0__:message"
  3) "set"
  
  1) "message"
  2) "__keyspace@0__:message"
  3) "expire"
  
  1) "message"
  2) "__keyspace@0__:message"
  3) "del"
  ```

  通知显示: 服务器先后对键`message`执行了`SET`,`EXPIRE`,`DEL`命令

- `键事件通知:` 关注某个命令执行在了什么键上.

  `SUBSCRIBE __keyevent@数据库编号__:操作名称`

  ```shell
  127.0.0.1:6379>SUBSCRIBE __keyspace@0__:message
  Reading messages...(press Ctrl-C to quit)
  1) "subscribe"
  2) "__keyevent@0__:del"
  3) (integer) 1
  
  1) "message"
  2) "__keyevent@0__:del"
  3) "message"
  
  1) "message"
  2) "__keyevent@0__:del"
  3) "number"
  
  1) "message"
  2) "__keyevent@0__:del"
  3) "redis"
  ```

  通知显示: 服务器先后在`message`, `number`, `redis` 上执行了`DEL 命令`.



**`服务器配置中notify-keyspace-events选项决定了服务器发送通知的类型:`**

1. `AKE`: 服务器会发送所有类型的键空间通知和键事件通知.
2. `AK`: 服务器会发送所有类型的间空间通知.
3. `AE`: 服务器会发送所有类型的键事件通知.
4. `K$`: 服务器只发送和字符串键有关的键空间通知.
5. `El`: 服务器只发送和列表键有关的键事件通知.

> 这个配置还有很多的可选值, 详见: [3.0redis配置文件](https://raw.githubusercontent.com/redis/redis/3.0/redis.conf)



#### 1. 发送通知:

发送通知功能由 `notify.c/notifyKeyspaceEvent`函数实现:

```c
void notifyKeyspaceEvent(int type, char *event, robj *key, int dbid)
```

- type: 发送通知的类型, 程序根据这个值来判断该通知是否是服务器配置notify-keyspace-events选项所配置的通知类型, 从而`决定是否发送通知`
- event: 时间名称
- key: 产生时间的键.
- dbid: 数据库编号

> 每当一个Redis命令需要发送数据库事件的时候, 就会调用一次这个函数.

***`实现:`***

```c
void notifyKeyspaceEvent(int type, char *event, robj *key, int dbid) {
    sds chan;
    robj *chanobj, *eventobj;
    int len = -1;
    char buf[24];

    /* If notifications for this class of events are off, return ASAP. */
    // 如果服务器配置为不发送 type 类型的通知，那么直接返回
    if (!(server.notify_keyspace_events & type)) return;

    // 事件的名字
    eventobj = createStringObject(event,strlen(event));

    /* __keyspace@<db>__:<key> <event> notifications. */
    // 发送键空间通知
    if (server.notify_keyspace_events & REDIS_NOTIFY_KEYSPACE) {

        // 构建频道对象
        chan = sdsnewlen("__keyspace@",11);
        len = ll2string(buf,sizeof(buf),dbid);
        chan = sdscatlen(chan, buf, len);
        chan = sdscatlen(chan, "__:", 3);
        chan = sdscatsds(chan, key->ptr);

        chanobj = createObject(REDIS_STRING, chan);

        // 通过 publish 命令发送通知
        pubsubPublishMessage(chanobj, eventobj);

        // 释放频道对象
        decrRefCount(chanobj);
    }

    /* __keyevente@<db>__:<event> <key> notifications. */
    // 发送键事件通知
    if (server.notify_keyspace_events & REDIS_NOTIFY_KEYEVENT) {

        // 构建频道对象
        chan = sdsnewlen("__keyevent@",11);
        // 如果在前面发送键空间通知的时候计算了 len ，那么它就不会是 -1
        // 这可以避免计算两次 buf 的长度
        if (len == -1) len = ll2string(buf,sizeof(buf),dbid);
        chan = sdscatlen(chan, buf, len);
        chan = sdscatlen(chan, "__:", 3);
        chan = sdscatsds(chan, eventobj->ptr);

        chanobj = createObject(REDIS_STRING, chan);

        // 通过 publish 命令发送通知
        pubsubPublishMessage(chanobj, key);

        // 释放频道对象
        decrRefCount(chanobj);
    }

    // 释放事件对象
    decrRefCount(eventobj);
}
```



## 10 RDB持久化:

由于redis是内存数据库, 它将自己的数据库状态存储在内存里面, 所以如果不想办法将数据持久化到磁盘里面, 那么一单服务器进程退出, 服务器中的数据库状态也会消失不见.

`RDB持久化`, 就是为了解决服务器进程退出数据库状态可以保存到磁盘里面, 避免数据丢失.

`RDB持久化`, 可以手动执行, 也可以在`redis.conf`配置文件中配置定期执行, 该功能可以将`某个时间点上的数据库状态(全量数据)保存到一个RDB文件中`.

`RDB文件是一个经过压缩的二进制文件, 通过该文件可以还原生成RDB文件时的数据库状态.`

### 10.1 RDB文件的创建和载入:

***`创建RDB文件:`***

- 手动

  通过`SAVE`和`BGSAVE`这两个命令, 可以生成redis服务器的RDB文件.

  - `SAVE命令`: 该命令会阻塞Redis服务器进程, 直到所有数据都写入到RDB文件里面后, 服务器才能处理客户端的请求.
  - `BGSAVE命令`: BGSAVE和SAVE同的点在于, `BGSAVE不会阻塞Redis服务进程`, BGSAVE命令会派生出一个子进程, 然后由子进程负责创建RDB文件, 服务器进程继续处理客户端的命令请求.

- 自动:

  通过redis.conf配置文件中save配置项, 就可以让Redis服务器每个一段时间自动执行一次`BGSAVE命令`来创建RDB文件.

  `save 间隔时间 修改次数`, 默认save配置:

  ```c
  save 900 1 // 服务器在900秒之内, 对数据库进行了1次修改
  save 300 10 // 服务器在300秒之内, 对数据库进行了10次修改
  save 60 10000 // 服务器在60秒之内, 对数据库进行了10000次修改
  ```

  >  `只要上面配置中任意一个条件被满足, 服务器就会执行BGSAVE命令`

***`加载RDB文件:`***

Redis并没有给RDB文件载入设置专门的命令, 而是在Redis服务器启动的时候, 只要检测到有RDB文件存在就会自动加载RDB文件.

> 由于AOF文件的更新频率通常比RDB文件更高, 因此:
>
> - 如果服务器开启了AOF持久化功能, 那么服务器会优先使用AOF文件来还原数据库状态.
> - 只有在AOF持久化功能处于关闭的状态时, 服务器才会使用RDB文件来还原数据库状态.

#### 1. save命令执行时服务器状态:

save命令会阻塞服务器主进程, 但save命令执行的时候, 客户端发送的所有命令请求都会被拒绝.

只有在服务器执行完save命令后, 重新开始接受请求之后, 客户端发送的命令才会被处理.

#### 2. bgsave命令执行时服务器状态:

bgsave命令在执行的时候, 会fork出一个子进程来处理RDB文件的保存工作, Redis服务器的主进程仍然可以处理客户端的命令请求.

但是, BGSAVE命令在执行时, 服务器会采用与BGSAVE命令没有执行时不同的方式处理`SAVE`, `BGSAVE`,`BGREWRITEAOF`三个命令:

- `SAVE`: 在BGSAVE执行期间, `服务器会拒绝执行SAVE命令`.目的是为了防止主进程执行的save操作与子进程执行的bgsave同时调用rdbSave, 产生竞争条件.

- `BGSAVE`: 在BGSAVE执行期间, `服务器会拒绝执行BGSAVE命令`. 目的是为了防止fork出来的两个子进程同时调用rdbSave, 产生竞争条件.

- `BGREWRITEAOF`: 

  - 如果服务器接收到BGREWRITEAOF时, BGSAVE正在执行, 那么BGREWRITEAOF命令会被延迟到BGSAVE命令执行完毕之后执行
  - 如果服务器接收到BGSAVE命令时, BGREWRITEAOF正在执行, 那么服务器会拒绝执行BGSAVE.

  > BGSAVE和BGREWRITEAOF都会fork出一个子进程进行持久化操作, 处于性能方面考虑 --- 并发出两个子进程同时执行大量的磁盘操作效率不高.

#### 3. RDB文件载入时服务器的状态:

服务器在载入RDB文件期间, `会一直处于阻塞状态`, 直到载入工作完成为止.



### 10.2 自动间隔性保存实现:

通过redis.conf配置文件中save配置项, 可以配置如果某个时间段内服务器对数据库执行了多少次修改, 服务器自动执行BGSAVE. 下面就是如何实现的.

#### 1. 设置保存条件:

redis.conf配置文件中save配置项, 用户如果没有配置的话, 会采用如下的默认配置:

```c
save 900 1 // 服务器在900秒之内, 对数据库进行了1次修改
save 300 10 // 服务器在300秒之内, 对数据库进行了10次修改
save 60 10000 // 服务器在60秒之内, 对数据库进行了10000次修改
```

在表示服务器的结构体中`redisServer`结构的saveparams属性, 就会将`save`配置项保存起来.

```c
struct redisServer {
    // ...
	// 记录save配置项的值
    struct saveparam *saveparams;
    // ...
}
```

saveparams是一个数组, 每一个元素都是一个`saveparam结构`:

```c
struct saveparam {
    // 描述
    time_t seconds;
    // 修改次数
    int changes;
}
```



#### 2. dirty计数和lastsave属性:

除了saveparams数组之外, redisServer还维护着一个dirty计数器, 以及一个lastsave属性:

```c
struct redisServer {
    // ...
	// 修改计数器
    long long dirty;
    // 上一次成功执行保存命令的时间:
    time_t lastsave;
    // ...
}
```

- dirty计: 记录的时距离上次成功执行save命令或者bgsave命令之后, 服务器对数据库状态(所有数据库) 进行了多少次修改(写入删除更新)
- lastsave: 是一个UNIX时间戳, 记录了服务器上一次成功执行save或者bgsave命令的时间 



#### 3. 检查是否满足条件:

Redis的服务器周期性操作函数serverCron默认每隔100毫秒就会执行一次, 该函数用于对redis服务器进行维护.检查是否满足条件的伪代码:

```c
for (j = 0; j < server.saveparamslen; j++) {
            struct saveparam *sp = server.saveparams+j;

            /* Save if we reached the given amount of changes,
             * the given amount of seconds, and if the latest bgsave was
             * successful or if, in case of an error, at least
             * REDIS_BGSAVE_RETRY_DELAY seconds already elapsed. */
            // 检查是否有某个保存条件已经满足了
    		// 1. 服务器修改数据库的次数 > save配置项的修改次数
    		// 2. 现在距离上次完成save的时间间隔 > save配置项的时间间隔
    		// 3. 现在距离上次尝试save的时间间隔 > save失败延迟常量 或者 最后一次执行 SAVE 的状态 为 REDIS_OK(0)
            if (server.dirty >= sp->changes &&
                server.unixtime-server.lastsave > sp->seconds &&
                (server.unixtime-server.lastbgsave_try >
                 REDIS_BGSAVE_RETRY_DELAY ||
                 server.lastbgsave_status == REDIS_OK))
            {
                redisLog(REDIS_NOTICE,"%d changes in %d seconds. Saving...",
                    sp->changes, (int)sp->seconds);
                // 执行 BGSAVE
                rdbSaveBackground(server.rdb_filename);
                break;
            }
         }
```





### 10.3 RDB文件:

主要介绍RDB文件的结构和意义.

主体结构如下:

![image-20221121224728309](E:\code\JavaStudySummary\docs\_content\chapter01_redis\redis设计与实现\test\image-20221121224728309.png)

- `REDIS`: RDB文件的最开头时`REDIS`部分, 这个部分的长度为5字节, 保存着`REDIS`五个字符. 通过这五个字符, 程序可以在载入文件时, 快速检查所载入的文件是否是RDB文件.

  > RDB文件存储的是二进制数据, 而不是c字符串, 只是为了方便理解, 采用c字符串.

- `db_version`: 长度为4字节, 它的值时一个字符串表示的证书, 这个整数记录了RDB文件的版本号, 比如"0006"就代表RDB文件的版本为第六版. `后面所讲的都是第六版的结构`.

- `database`部分包含着领个或者任意多个数据库, 以及各个数据库中的键值对数据:

  - 如果服务器的数据库状态为空(所有数据库都是空的), 那么这个部分也为空, 长度为0字节．
  - 如果服务器的数据库状态为非空(有至少一个数据库非空), 那么这个部分也非空, 根据数据库所保存键值对的数量, 类型和内容不同, 这部分的长度也不同.

- `EOF`: EOF常量的长度为1字字节, 这个常量标志着RDB文件正文内容的结束, 当读入程序遇到这个值的时候, 程序就知道所有数据库的所有键值对都已经载入完毕.

- `check_sum`: 是一个8字节长度的无符号证书, 保存着一个校验和, 这个校验和是通过前面四个部分的内容计算得到的. `主要时用来检查数据RDB文件是否损坏`.



#### 1. databases部分:

一个RDB文件的databases部分可以保存任意多个非空数据库.

举个例子:

服务器的0,3号数据库为非空, 那么RDB文件结构如图:

![image-20221121230146189](E:\code\JavaStudySummary\docs\_content\chapter01_redis\redis设计与实现\test\image-20221121230146189.png)

`每个非空数据库在RDB文件中都可以保存为SELECTDB, db_number, key_value_pairs三部分`:

![image-20221121230302419](E:\code\JavaStudySummary\docs\_content\chapter01_redis\redis设计与实现\test\image-20221121230302419.png)

- `SELECTDB`: 是个常量长度为1字节, 当读入程序遇到这个值的时候, 就明白接下来将要读入的僵尸一个数据库号码.
- `db_number`: 保存着一个数据库号码, 根据号码的大小不同, 这个部分长度可以是1字节, 2字节, 或者5字节. 读入db_number部分之后, 服务器调用select命令, 根据读入的数据库号码进行数据库切换, 从而将读入的键值对可以加载到对应的rdb文件的数据库结构中
- `key_value_pairs`: 保存了数据库中所有的键值对数据, 如果键值对带有过期时间, 那么过期时间也会和键值对保存在一起.



**`完整RBD结构`**

![image-20221121230939496](E:\code\JavaStudySummary\docs\_content\chapter01_redis\redis设计与实现\test\image-20221121230939496.png)



#### 2. key_value_pairs部分:

RDB文件中每个`key_value_pairs`部分都保存了一个或以上数据的键值对, 如果键值对带有过期时间的话, 那么过期时间也会一起记录

![image-20221121231155754](E:\code\JavaStudySummary\docs\_content\chapter01_redis\redis设计与实现\test\image-20221121231155754.png)

- `TYPE`: 记录了value的类型, 长度为1字节, 值可以是以下常量中的一个:

  ```c
  #define REDIS_RDB_TYPE_STRING 0
  #define REDIS_RDB_TYPE_LIST   1
  #define REDIS_RDB_TYPE_SET    2
  #define REDIS_RDB_TYPE_ZSET   3
  #define REDIS_RDB_TYPE_HASH   4
  #define REDIS_RDB_TYPE_LIST_ZIPLIST  10
  #define REDIS_RDB_TYPE_SET_INTSET    11
  #define REDIS_RDB_TYPE_ZSET_ZIPLIST  12
  #define REDIS_RDB_TYPE_HASH_ZIPLIST  13
  ```

  这些常量代表的是一种对象类型或者底层编码, 当服务器读入RDB文件中的键值对数据时, 程序会`根据TYPE的值来决定如何读入和解释`value的数据.

- `key`: key总是一个字符串对象, 它的编码方式和`REDIS_RDB_TYPE_STRING`的value是一样的, 内容长度不同, key部分的长度也会不同.

- `value`: 根据TYPE类型不同, 以及保存内容长度的不同, 保存vlaue的结构和长度也会有所不同.

- `EXPIRETIME_MS`: 是个常量, 长度为1字节, 作用是`告知读入程序, 接下来要读入的将是一个以毫秒为单位的过期时间`.

- `ms`: 是一个8字节长度的带符号整数, 记录着一个以毫秒为单位的UNIX时间戳, 这个字段表示的就是`键值对的过期时间`.

> key, value, type三个字段时一定有的. `EXPIRETIME_MS`, `ms`只有键值对带有过期时间的时候才会存在



#### 3. value的编码:

RDB文件中的每个value部分都保存了一个值对象, 每个值对象的类型都由与之对应的TYPE记录, TYPE不同value部分的结构, 长度也会有所不同.