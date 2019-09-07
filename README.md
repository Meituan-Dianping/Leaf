# Leaf

> There are no two identical leaves in the world.
>
> 世界上没有两片完全相同的树叶。
>
> ​								— 莱布尼茨

## Introduction

Leaf 最早期需求是各个业务线的订单ID生成需求。在美团早期，有的业务直接通过DB自增的方式生成ID，有的业务通过redis缓存来生成ID，也有的业务直接用UUID这种方式来生成ID。以上的方式各自有各自的问题，因此我们决定实现一套分布式ID生成服务来满足需求。具体Leaf 设计文档见：[ leaf 美团分布式ID生成服务 ](https://tech.meituan.com/MT_Leaf.html )

目前Leaf覆盖了美团点评公司内部金融、餐饮、外卖、酒店旅游、猫眼电影等众多业务线。在4C8G VM基础上，通过公司RPC方式调用，QPS压测结果近5w/s，TP999 1ms。

## Quick Start

### Leaf Server

我们提供了一个基于spring boot的HTTP服务来获取ID

#### 运行Leaf Server

##### 打包服务

```shell
cd leaf
mvn clean install -DskipTests
cd leaf-server
```

##### 运行服务
###### mvn方式

```shell
mvn spring-boot:run
```

###### 脚本方式

```shell
sh deploy/run.sh
```
##### 测试

```shell
#segment
curl http://localhost:8080/api/segment/get/leaf-segment-test
#snowflake
curl http://localhost:8080/api/snowflake/get/test
```
#### 配置介绍

Leaf 提供两种生成的ID的方式（号段模式和snowflake模式），你可以同时开启两种方式，也可以指定开启某种方式（默认两种方式为关闭状态）。

Leaf Server的配置都在leaf-server/src/main/resources/leaf.properties中

| 配置项                    | 含义                          | 默认值 |
| ------------------------- | ----------------------------- | ------ |
| leaf.name                 | leaf 服务名                   |        |
| leaf.segment.enable       | 是否开启号段模式              | false  |
| leaf.jdbc.url             | mysql 库地址                  |        |
| leaf.jdbc.username        | mysql 用户名                  |        |
| leaf.jdbc.password        | mysql 密码                    |        |
| leaf.snowflake.enable     | 是否开启snowflake模式         | false  |
| leaf.snowflake.zk.address | snowflake模式下的zk地址       |        |
| leaf.snowflake.port       | snowflake模式下的服务注册端口 |        |

#### 号段模式

如果使用号段模式，需要建立DB表，并配置leaf.jdbc.url, leaf.jdbc.username, leaf.jdbc.password

如果不想使用该模式配置leaf.segment.enable=false即可。

##### 创建数据表

```sql
CREATE DATABASE leaf
CREATE TABLE `leaf_alloc` (
  `biz_tag` varchar(128)  NOT NULL DEFAULT '',
  `max_id` bigint(20) NOT NULL DEFAULT '1',
  `step` int(11) NOT NULL,
  `description` varchar(256)  DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB;

insert into leaf_alloc(biz_tag, max_id, step, description) values('leaf-segment-test', 1, 2000, 'Test leaf Segment Mode Get Id')
```

##### 配置相关数据项

在leaf.properties中配置leaf.jdbc.url, leaf.jdbc.username, leaf.jdbc.password参数

#### Snowflake模式

算法取自twitter开源的snowflake算法。

如果不想使用该模式配置leaf.snowflake.enable=false即可。

##### 配置zookeeper地址

在leaf.properties中配置leaf.snowflake.zk.address，配置leaf 服务监听的端口leaf.snowflake.port。

##### 监控页面

号段模式：http://localhost:8080/cache

### Leaf Core

当然，为了追求更高的性能，需要通过RPC Server来部署Leaf 服务，那仅需要引入leaf-core的包，把生成ID的API封装到指定的RPC框架中即可。

### Leaf Client

引入nacos，可以启动多个实例，将服务注册到nacos中，进行负载均衡，提高服务的可用性

nacos home : https://nacos.io/zh-cn/docs/quick-start.html

使用服务时，不用手动进行http访问，只需引入leaf-client
```java
LeafClient leafClient = new LeafClient("nacos_addr","server_name");
String res = leafClient.request("method_pattern");

note:
    nacos_addr : nacos服务地址
    server_name: 服务名
    method_pattern: 请求服务下指定方法
eg:
    LeafClient leafClient = new LeafClient("127.0.0.1:8848","mall-leaf");
    String res = leafClient.request("/api/snowflake/get");
```
