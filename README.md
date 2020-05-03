针对Leaf原项目中的一些issue，对Leaf项目进行功能加强和修复，增加了对非Zookeeper注册中心的支持和性能优化，主要改进如下：

### Snowflake算法相关的改进：

#### 1.针对Leaf原项目中的[issue#84](https://github.com/Meituan-Dianping/Leaf/issues/84)，增加zk_recycle模式（注册中心为zk，workId循环使用）

#### 2.针对Leaf原项目中的[issue#100](https://github.com/Meituan-Dianping/Leaf/issues/100)，增加MySQL模式(注册中心为MySQL)

#### 3.针对Leaf原项目中的[issue#100](https://github.com/Meituan-Dianping/Leaf/issues/100)，增加Local模式(注册中心为本地项目配置)

### Segement生成ID相关的改进：

#### 1.针对Leaf原项目中的[issue#68](https://github.com/Meituan-Dianping/Leaf/issues/68)，优化SegmentIDGenImpl.updateCacheFromDb()方法。

#### 2.针对Leaf原项目中的 [issue#88](https://github.com/Meituan-Dianping/Leaf/issues/88)，使用位运算&替换取模运算

## snowflake算法生成ID的相关改进

Leaf项目原来的注册中心的模式(我们暂时命令为zk_normal模式)
使用Zookeeper作为注册中心，每次机器启动时去Zookeeper特定路径下读取子节点列表，如果存在当前IP:Port对应的workId，就使用节点信息中存储的workId，不存在就创建一个永久有序节点，将序号作为workId，并且将workId信息写入本地缓存文件workerID.properties，供启动时连接Zookeeper失败，读取使用。

### 1.针对Leaf原项目中的[issue#84](https://github.com/Meituan-Dianping/Leaf/issues/84)，增加zk_recycle模式（注册中心为zk，workId循环使用）

针对使用snowflake生成分布式ID的技术方案，原本是使用Zookeeper作为注册中心为每个服务根据IP:Port分配一个固定的workId，workId生成范围为0到1023，workId不支持回收，所以在Leaf的原项目中有人提出了一个issue[#84 workid是否支持回收？](https://github.com/Meituan-Dianping/Leaf/issues/84)，因为当部署Leaf的服务的IP和Port不固定时，如果workId不支持回收，当workId超过最大值时，会导致生成的分布式ID的重复。所以增加了workId循环使用的模式zk_recycle。
#### 如何使用zk_recycle模式?
在Leaf/leaf-server/src/main/resources/leaf.properties中添加以下配置
```
//开启snowflake服务
leaf.snowflake.enable=true
//leaf服务的端口，用于生成workId
leaf.snowflake.port=
//将snowflake模式设置为zk_recycle，此时注册中心为Zookeeper，并且workerId可复用
leaf.snowflake.mode=zk_recycle
//zookeeper的地址
leaf.snowflake.zk.address=localhost:2181
```
启动LeafServerApplication，调用/api/snowflake/get/test就可以获得此种模式下生成的分布式ID。
```
curl domain/api/snowflake/get/test
1256557484213448722
```
#### zk_recycle模式实现原理
按照上面的配置在leaf.properties里面进行配置后，
```
if(mode.equals(SnowflakeMode.ZK_RECYCLE)) {//注册中心为zk,对ip:port分配的workId是课循环利用的模式
     String    zkAddress = properties.getProperty(Constants.LEAF_SNOWFLAKE_ZK_ADDRESS);
     RecyclableZookeeperHolder holder    = new RecyclableZookeeperHolder(Utils.getIp(),port,zkAddress);
     idGen = new SnowflakeIDGenImpl(holder);
     if (idGen.init()) {
     logger.info("Snowflake Service Init Successfully in mode " + mode);
     } else {
     throw new InitException("Snowflake Service Init Fail");
     }
}
```
此时SnowflakeIDGenImpl使用的holder是RecyclableZookeeperHolder的实例，workId是可循环利用的，RecyclableZookeeperHolder工作流程如下：
1.首先会在未使用的workId池(zookeeper路径为/snowflake/leaf.name/recycle/notuse/)中生成所有workId。
2.然后每次服务器启动时都是去未使用的workId池取一个新的workId，然后放到正在使用的workId池(zookeeper路径为/snowflake/leaf.name/recycle/inuse/)下，将此workId用于Id生成，并且定时上报时间戳，更新zookeeper中的节点信息。
3.并且定时检测正在使用的workId池，发现某个workId超过最大时间没有更新时间戳的workId，会把它从正在使用的workId池移出，然后放到未使用的workId池中，以供workId循环使用。
4.并且正在使用这个很长时间没有更新时间戳的workId的服务器，在发现自己超过最大时间，还没有上报时间戳成功后，会停止id生成服务，以防workId被其他服务器循环使用，导致id重复。

### 2.针对Leaf原项目中的[issue#100](https://github.com/Meituan-Dianping/Leaf/issues/100)，增加MySQL模式(注册中心为MySQL)

注册中心为MySQL，针对每个ip:port的workid是固定的。
#### 如何使用这种mysql模式?
需要先在数据库执行项目中的leaf_workerid_alloc.sql，完成建表，然后在Leaf/leaf-server/src/main/resources/leaf.properties中添加以下配置
```
//开启snowflake服务
leaf.snowflake.enable=true
//leaf服务的端口，用于生成workId
leaf.snowflake.port=
//将snowflake模式设置为mysql，此时注册中心为Zookeeper，workerId为固定分配
leaf.snowflake.mode=mysql
//mysql数据库地址
leaf.jdbc.url=
leaf.jdbc.username=
leaf.jdbc.password=
```
启动LeafServerApplication，调用/api/snowflake/get/test就可以获得此种模式下生成的分布式ID。
```
curl domain/api/snowflake/get/test
1256557484213448722
```
#### 实现原理
使用上面的配置后，此时SnowflakeIDGenImpl使用的holder是SnowflakeMySQLHolder的实例。实现原理与Leaf原项目默认的模式，使用Zookeeper作为注册中心，每个ip:port的workid是固定的实现原理类似，只是注册，获取workid，及更新时间戳是与MySQL进行交互，而不是Zookeeper。
```
if (mode.equals(SnowflakeMode.MYSQL)) {//注册中心为mysql
		DruidDataSource dataSource = new DruidDataSource();
		dataSource.setUrl(properties.getProperty(Constants.LEAF_JDBC_URL));
dataSource.setUsername(properties.getProperty(Constants.LEAF_JDBC_USERNAME));
dataSource.setPassword(properties.getProperty(Constants.LEAF_JDBC_PASSWORD));
		dataSource.init();
		// Config Dao
		WorkerIdAllocDao dao = new WorkerIdAllocDaoImpl(dataSource);
		SnowflakeMySQLHolder holder = new SnowflakeMySQLHolder(Utils.getIp(), port, dao);
		idGen = new SnowflakeIDGenImpl(holder);
		if (idGen.init()) {
				logger.info("Snowflake Service Init Successfully in mode " + mode);
		} else {
				throw new InitException("Snowflake Service Init Fail");
    }
}
```

### 3.针对Leaf原项目中的[issue#100](https://github.com/Meituan-Dianping/Leaf/issues/100)，增加Local模式(注册中心为本地项目配置)

这种模式就是适用于部署Leaf服务的IP和Port基本不会变化的情况，就是在Leaf项目中的配置文件leaf.properties中显式得配置某某IP:某某Port对应哪个workId，每次部署新机器时，将IP:Port的时候在项目中添加这个配置，然后启动时项目会去读取leaf.properties中的配置，读取完写入本地缓存文件workId.json，下次启动时直接读取workId.json，最大时间戳也每次同步到机器上的缓存文件workId.json中。
#### 如何使用这种local模式?
在Leaf/leaf-server/src/main/resources/leaf.properties中添加以下配置
```
//开启snowflake服务
leaf.snowflake.enable=true
//leaf服务的端口，用于生成workId
leaf.snowflake.port=
#注册中心为local的的模式
#leaf.snowflake.mode=local
#leaf.snowflake.local.workIdMap=
#workIdMap的格式是这样的{"Leaf服务的ip:端口":"固定的workId"},例如:{"10.1.46.33:8080":1,"10.1.46.33:8081":2}
```
启动LeafServerApplication，调用/api/snowflake/get/test就可以获得此种模式下生成的分布式ID。
```
curl domain/api/snowflake/get/test
1256557484213448722
```
## 针对Segement生成分布式ID相关的改进

### 1.针对Leaf原项目中的[issue#68](https://github.com/Meituan-Dianping/Leaf/issues/68)，优化SegmentIDGenImpl.updateCacheFromDb()方法

针对[issue#68](https://github.com/Meituan-Dianping/Leaf/issues/68)里面的问题，对Segement Buffer的缓存数据与DB数据同步的工作流程进行优化，主要是对
对SegmentIDGenImpl.updateCacheFromDb()方法进行了优化。

原方案工作流程:
       1.遍历cacheTags，将dbTags的副本insertTagsSet中存在的元素移除，使得insertTagsSet只有db新增的tag
     2.遍历insertTagsSet，将这些新增的元素添加到cache中
       3.遍历dbTags，将cacheTags的副本removeTagsSet中存在的元素移除，使得removeTagsSet只有cache中过期的tag
       4.遍历removeTagsSet，将过期的元素移除cache
       这种方案需要经历四次循环，使用两个HashSet分别存储db中新增的tag，cache中过期的tag，
       并且为了筛选出新增的tag，过期的tag，对每个现在使用的tag有两次删除操作，
      
原有方案代码如下：
```
            List<String> dbTags = dao.getAllTags();
            if (dbTags == null || dbTags.isEmpty()) {
                return;
            }
            List<String> cacheTags = new ArrayList<String>(cache.keySet());
            Set<String> insertTagsSet = new HashSet<>(dbTags);
            Set<String> removeTagsSet = new HashSet<>(cacheTags);
            //db中新加的tags灌进cache
            for(int i = 0; i < cacheTags.size(); i++){
                String tmp = cacheTags.get(i);
                if(insertTagsSet.contains(tmp)){
                    insertTagsSet.remove(tmp);
                }
            }
            for (String tag : insertTagsSet) {
                SegmentBuffer buffer = new SegmentBuffer();
                buffer.setKey(tag);
                Segment segment = buffer.getCurrent();
                segment.setValue(new AtomicLong(0));
                segment.setMax(0);
                segment.setStep(0);
                cache.put(tag, buffer);
                logger.info("Add tag {} from db to IdCache, SegmentBuffer {}", tag, buffer);
            }
            //cache中已失效的tags从cache删除
            for(int i = 0; i < dbTags.size(); i++){
                String tmp = dbTags.get(i);
                if(removeTagsSet.contains(tmp)){
                    removeTagsSet.remove(tmp);
                }
            }
            for (String tag : removeTagsSet) {
                cache.remove(tag);
                logger.info("Remove tag {} from IdCache", tag);
            }
```

实际上我们并不需要这些中间过程，现方案工作流程：
       只需要遍历dbTags，判断cache中是否存在这个key，不存在就是新增元素，进行新增。
       遍历cacheTags，判断dbSet中是否存在这个key，不存在就是过期元素，进行删除。

现有方案代码：
```
            List<String> dbTags = dao.getAllTags();
            if (dbTags == null || dbTags.isEmpty()) {
                return;
            }
            //将dbTags中新加的tag添加cache，通过遍历dbTags，判断是否在cache中存在，不存在就添加到cache
            for (String dbTag : dbTags) {
                if (cache.containsKey(dbTag)==false) {
                    SegmentBuffer buffer = new SegmentBuffer();
                    buffer.setKey(dbTag);
                    Segment segment = buffer.getCurrent();
                    segment.setValue(new AtomicLong(0));
                    segment.setMax(0);
                    segment.setStep(0);
                    cache.put(dbTag, buffer);
                    logger.info("Add tag {} from db to IdCache, SegmentBuffer {}", dbTag, buffer);
                }
            }
            List<String> cacheTags = new ArrayList<String>(cache.keySet());
            Set<String>  dbTagSet     = new HashSet<>(dbTags);
            //将cache中已失效的tag从cache删除，通过遍历cacheTags，判断是否在dbTagSet中存在，不存在说明过期，直接删除
            for (String cacheTag : cacheTags) {
                if (dbTagSet.contains(cacheTag) == false) {
                    cache.remove(cacheTag);
                    logger.info("Remove tag {} from IdCache", cacheTag);
                }
            }
```

两个方案对比：
* 空间复杂度
相比原方案需要使用两个HashSet，这种方案的只需要使用一个hashSet，空间复杂度会低一些。
* 时间复杂度
总遍历次数会比原来的少，时间复杂度更低，因为判断是新增，过期的情况就直接处理了，不需要后续再单独遍历，
而且不需要对cache和dbtag的交集进行删除操作，因为原来方案为了获得新增的元素，是将dbSet的副本中现有元素进行删除得到。
* 代码可读性
原方案是4个for循环，总共35行代码，现方案是2个for循环，总共25行代码，更加简洁易懂。

### 2.针对Leaf原项目中的[issue#88](https://github.com/Meituan-Dianping/Leaf/issues/88)，使用位运算&替换取模运算

这个更新是针对这个[issue#88](https://github.com/Meituan-Dianping/Leaf/issues/88) ，使用位运算&来代替取模运算%，执行效率更高。
原代码：

```
public int nextPos() {
        return (currentPos + 1) % 2;
}
```
现代码：
```
public int nextPos() {
        return (currentPos + 1) & 1;
}
```
