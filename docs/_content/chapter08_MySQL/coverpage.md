## docker 安装mysql:

使用的是docker compose安装

首先创建需要映射的文件夹:

```bash
mkdir data && mkdir logs && mkdir conf
```

编辑docker-compose.yml文件

```yml
version: '3.5'
services:
  mysql:
    image: mysql:5.7.35
    container_name: mysql
    restart: always
    ports:
      - 33066:3306
    volumes:
      - /home/pounds/1soft/dockerData/mysql/data:/usr/local/mysql
      - /home/pounds/1soft/dockerData/mysql/conf/my.cnf:/etc/mysql/my.cnf
      - /home/pounds/1soft/dockerData/mysql/logs:/usr/local/mysql/logs
    environment:
      TZ: Asia/Shanghai
      MYSQL_ROOT_PASSWORD: root
    command:
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_general_ci
      --default-authentication-plugin=mysql_native_password 
```

mysql配置文件:

```c
[mysqld]
# binlog 配置
log-bin=/usr/local/mysql/logs/bin-log/mysql-bin
expire-logs-days=14
max-binlog-size=500M
server-id=1
# 跳过mysql解析IP
skip-name-resolve
# GENERAL
datadir=/usr/local/mysql/data
socket=/usr/local/mysql/mysql.sock
user=mysql
default-storage-engine=InnoDB
character-set-server=utf8mb4
lower_case_table_names = 1
explicit_defaults_for_timestamp=true
[mysqld_safe]
log-error=/usr/local/mysql/logs/err-log/mysql-error.log
pid-file=/usr/local/mysql/mysqld.pid
[client]
socket=/usr/local/mysql/mysql.sock
[mysql]
default-character-set=utf8mb4
socket=/usr/local/mysql/mysql.sock
```

问题记录:

1. 挂载文件的问题:

   挂载的时候要注意挂载路径对应的文件是否存在,权限是否给到docker里面的用户,不然容易出现挂载文件无法读写的问题

2. docker-compose设置的环境变量无法使用问题:

   docker compose的设置只是对新创建的容器生效, 如果挂载的mysql data文件中存在了一个已经配置好了的mysql数据的话, docker-compose创建的mysql容器将会继续使用已有配置,而不是docker-compose中的配置.

   `暂时的解决办法: 创建新mysql容器可以把data文件夹下面的数据删了,如果想要沿用以前的配置的话, 可以进容器手动修改配置`

3. `[Can't find messagefile '\usr\local\mysql\share\errmsg.sys']`:

   这个是因为在配置文件中设置了 `basedir`, 导致mysql容器在从basedir下面读取不到errmsg.sys文件..

   `暂时的解决办法是: 不修改mysql容器的basedir配置`

4. `mysql-bin.index`文件不存在:

   解决办法: 在设置mysql配置`log-bin`binlog文件存储路径的时候,不设置到文件, 只设置到文件夹.MySQL加载的时候就会自动生成`mysql-bin.index`文件.