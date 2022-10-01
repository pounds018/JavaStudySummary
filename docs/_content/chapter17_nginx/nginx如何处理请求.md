# 基于名字的虚拟服务器

Nginx首先会决定那个服务器来处理请求.以下面这三个server配置为例:

```conf
server {
    listen      80;
    server_name example.org www.example.org;
    ...
}

server {
    listen      80;
    server_name example.net www.example.net;
    ...
}

server {
    listen      80;
    server_name example.com www.example.com;
    ...
}
```

以上的配置时, nginx只会根据请求头中`Host`字段的值来决定请求会被路由到那个服务器.如果请求头中`Host`的值与所有服务器都无法匹配或者请求头中根本就没有`Host`字段, 那么就会被路由到默认的server服务器上去.

nginx默认第一个配置的server服务器为默认服务器, 可以通过`default(0.8.21版本以前)`或者`defualt_server`来指定那个server是默认server,比如:

```conf
server {
    listen      80 default_server;
    server_name example.net www.example.net;
    ...
}
```

> default server是`listen port`的属性而不是`server name`的属性.

# 如何不处理为匹配的请求

如果请求头不带`Host`字段的请求做丢弃处理, 可以使用下面这个配置:

```conf
server {
    listen      80;
    server_name "";
    return      444;
}
```

当请求头中没有host字段的请求过来了, 名字设置为""的服务器会自动匹配,然后响应一个`444`代码

> 0.8.48版本之后, server name默认为"",所以可以不用设置.
>
> 0.8.48版本之前, server name 使用一个默认的名称, 必须要将server_name配置成""

# 名称和ip都有的虚拟服务器

```cong
server {
    listen      192.168.1.1:80;
    server_name example.org www.example.org;
    ...
}

server {
    listen      192.168.1.1:80;
    server_name example.net www.example.net;
    ...
}

server {
    listen      192.168.1.2:80;
    server_name example.com www.example.com;
    ...
}
```

当nginx配置如上图的时候, nginx会首先去匹配请求所请求的ip地址和端口, 再从ip和端口匹配上的所有server中, 找出server_name和请求头中host字段匹配的. 如果没有找到合适的server_name, 就会路由到ip和端口匹配的server中默认的server来处理.

```
服务器 {
    听 192.168.1.1:80; 
    server_name example.org www.example.org; 
    ... 
}

服务器 {
    听 192.168.1.1:80 default_server ; 
    server_name example.net www.example.net; 
    ... 
}

服务器 {
    听 192.168.1.2:80 default_server ; 
    server_name example.com www.example.com; 
    ... 
}
```

> 由于default_server是listen命令的参数, 所以可以设置多个ip:port不相同的defualt_server



# 服务器的名字:

通过`server_name`可以设置服务器名字, 服务器名字可能是 `固定名称`, `通配符名名称`, `正则表达式`

```conf
server {
    listen       80;
    server_name  example.org  www.example.org;
    ...
}

server {
    listen       80;
    server_name  *.example.org;
    ...
}

server {
    listen       80;
    server_name  mail.*;
    ...
}

server {
    listen       80;
    server_name  ~^(?<user>.+)\.example\.net$;
    ...
}
```

`server_name`不同设置方式的优先级不同:

1. 固定名字优先级最高
2. 后缀通配符名字,`*.example.com`
3. 前缀通配符名字, `com.example.*`
4. 正则表达式名字, 多个正则表达式都成功匹配的时候, 优先级由 server_name 在配置文件中出现的顺序从上到下依次降低.

## 通配符server_name:

通配符`*`只能在 名字的开头或者结尾使用, 且只能挨着`.`符号. 无效名称比如: `www.*.example.com`,`w*.example.com`. 如果一定要这样写, 可以使用正则表达式来处理. `~^www\..+\.example\.org$`” and和“`~^w.*\.example\.org$`”

`*`匹配的不仅仅是一个`单词`或者`.`, 比如: `*.example.com`只要是以 `exmple.com`结尾的都能匹配上.

特殊的通配符名称: `.example.com` 可以匹配 `example.com`或者`*.example.com`.