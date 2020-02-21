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

Leaf provides an HTTP service based on spring boot to get the id

#### run Leaf Server

##### build

```shell
git clone git@github.com:Meituan-Dianping/Leaf.git
cd leaf
mvn clean install -DskipTests
cd leaf-server
```

##### run
###### maven

```shell
mvn spring-boot:run
```

or 
###### shell command

```shell
sh deploy/run.sh
```

##### test

```shell
#segment
curl http://localhost:8080/api/segment/get/leaf-segment-test
#snowflake
curl http://localhost:8080/api/snowflake/get/test
```

#### Configuration

Leaf provides two ways to generate ids (segment mode and snowflake mode), which you can turn on at the same time or specify one way to turn on (both are off by default).

Leaf Server configuration is in the leaf-server/src/main/resources/leaf.properties

| configuration             | meaning                          | default |
| ------------------------- | ----------------------------- | ------ |
| leaf.name                 | leaf service name                  |        |
| leaf.segment.enable       | whether segment mode is enabled             | false  |
| leaf.jdbc.url             | mysql url                 |        |
| leaf.jdbc.username        | mysql username                 |        |
| leaf.jdbc.password        | mysql password                   |        |
| leaf.snowflake.enable     | whether snowflake mode is enabled         | false  |
| leaf.snowflake.zk.address | zk address under snowflake mode      |        |
| leaf.snowflake.port       | service registration port under snowflake mode |        |

### Segment mode 

In order to use segment mode, you need to create DB table first, and configure leaf.jdbc.url, leaf.jdbc.username, leaf.jdbc.password

If you do not want use it, just configure leaf.segment.enable=false to disable it.

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
### Snowflake mode 

The algorithm is taken from twitter's open-source snowflake algorithm.

If you do not want to use it, just configure leaf.snowflake.enable=false to disable it.

Configure the zookeeper address

```
leaf.snowflake.zk.address=${address}
leaf.snowflake.enable=true
leaf.snowflake.port=${port}
```

configure leaf.snowflake.zk.address in the leaf.properties, and configure the leaf service listen port leaf.snowflake.port.

### monitor page

segment mode: http://localhost:8080/cache

### Leaf Core 

Of course, in order to pursue higher performance, you need to deploy the Leaf service through RPC Server, which only needs to introduce the leaf-core package and encapsulate the API that generates the ID into the specified RPC framework.

#### Attention
Note that leaf's current IP acquisition logic in the case of snowflake mode takes the first network card IP directly (especially for services that change IP) to avoid wasting the workId
