以手动搭建一个nginx日志监控系统为例子,学习elk技术栈

## 1. 轻量级数据采集器(Beat):

### 1. FileBeat日志采集器:

1. 简介:

   filebeat主要是对预先设置好的日志文件进行监控,然后将数据发送给logstash或者es的一个技术. 

2. 架构:

   ![image-20211003151853467](elk相关技术/image-20211003151853467.png)

3. docker安装filebeat:

4. 

