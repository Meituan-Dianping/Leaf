# Leaf

> There are no two identical leaves in the world.
>
> ​               — Leibnitz

[中文文档](./README_CN.md) | [English Document](./README.md)

## Introduction

Leaf refers to some common ID generation schemes in the industry, including redis, UUID, snowflake, etc.
Each of the above approaches has its own problems, so we decided to implement a set of distributed ID generation services to meet the requirements.
At present, Leaf covers Meituan review company's internal finance, catering, takeaway, hotel travel, cat's eye movie and many other business lines. On the basis of 4C8G VM, through the company RPC method, QPS pressure test results are nearly 5w/s, TP999 1ms.

You can use it to encapsulate a distributed unique id distribution center in a service-oriented SOA architecture as the id distribution provider for all applications

## Quick Start

### Leaf Server

Leaf provide an HTTP service based on spring boot to get the id
#### USEAGE
#### Segment MODE

##### 1.Create table on your MySQL server,SQL : 

```sql
CREATE DATABASE leaf
CREATE TABLE `leaf_alloc` (
  `biz_tag` varchar(128)  NOT NULL DEFAULT '', -- your biz unique name
  `max_id` bigint(20) NOT NULL DEFAULT '1',
  `step` int(11) NOT NULL,
  `description` varchar(256)  DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB;

insert into leaf_alloc(biz_tag, max_id, step, description) values('leaf-segment-test', 1, 2000, 'Test leaf Segment Mode Get Id')
```
##### 2.build Leaf Server

```
git clone https://github.com/Meituan-Dianping/Leaf.git
```
config your leaf.properties

```
leaf.name=com.sankuai.leaf.opensource.test
leaf.segment.enable=false
leaf.jdbc.url=
leaf.jdbc.username=
leaf.jdbc.password=

leaf.snowflake.enable=false
#leaf.snowflake.zk.address=
#leaf.snowflake.port=
© 2019 GitHub, Inc.
```

```shell
cd leaf
mvn clean install -DskipTests
cd leaf-server
```

##### 3.RUN IT
###### maven

```shell
mvn spring-boot:run
```
OR
###### shell command

```shell
sh deploy/run.sh
```
##### GET ID

```shell
#segment
curl http://localhost:8080/api/segment/get/leaf-segment-test
#snowflake
curl http://localhost:8080/api/snowflake/get/test
```
#### Config DESC

Leaf provides two ways to generate ids (segment mode and snowflake mode), which you can turn on at the same time or specify one way to turn on (both are off by default).

Leaf Server configuration in the Leaf - Server/SRC/main/resources/Leaf. The properties

| config                    | meaning                          | default |
| ------------------------- | ----------------------------- | ------ |
| leaf.name                 | leaf server name                  |        |
| leaf.segment.enable       | Whether segment mode is enabled             | false  |
| leaf.jdbc.url             | mysql url                 |        |
| leaf.jdbc.username        | mysql username                 |        |
| leaf.jdbc.password        | mysql password                   |        |
| leaf.snowflake.enable     | Whether snowflke mode is enabled         | false  |
| leaf.snowflake.zk.address |Zk address in snowflake mode      |        |
| leaf.snowflake.port       | Service registration port under snowflake mode |        |


### Snowflake model

The algorithm is taken from twitter's open-source snowflake algorithm.

If you do not want to configure leaf.snowflake. Enable =false with this mode.

Configure the zookeeper address

```
leaf.snowflake.zk.address=${address}
leaf.snowflake.enable=true
leaf.snowflake.port=${port}
```

In the leaf. The leaf that is configured in the properties. The snowflake. Zk. Address, configure the leaf service listen port leaf. Snowflake. Port.

##### leaf Monitor Page

View the current number generation of each key,Them roughly mode: http://localhost:8080/cache

### The Leaf to the Core

Of course, in order to pursue higher performance, you need to deploy the Leaf service through RPC Server, which only needs to introduce the leaf-core package and encapsulate the API that generates the ID into the specified RPC framework.
