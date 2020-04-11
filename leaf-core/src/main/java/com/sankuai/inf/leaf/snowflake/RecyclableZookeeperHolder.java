package com.sankuai.inf.leaf.snowflake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankuai.inf.leaf.common.PropertyFactory;
import com.sankuai.inf.leaf.snowflake.exception.CheckLastTimeException;
import com.sankuai.inf.leaf.snowflake.exception.NoUserfulWorkIdException;
import com.sankuai.inf.leaf.snowflake.exception.ZookeeperConnectFailException;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class RecyclableZookeeperHolder implements SnowflakeHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecyclableZookeeperHolder.class);

    private int workerID;
    private static final String PREFIX_ZK_PATH = "/snowflake/" + PropertyFactory.getProperties().getProperty("leaf.name");
    //未使用的workId池
    private static final String PATH_RECYCLE_NOTUSE = PREFIX_ZK_PATH + "/recycle/notuse";
    //正在使用的workId池
    private static final String PATH_RECYCLE_INUSE = PREFIX_ZK_PATH + "/recycle/inuse";

    private String ip;
    private String port;
    private String zkAddress;
    private Long lastUpdateTime = null;
    //最大的workId，默认是1023，用于确定未使用的workId池中初始化workId的个数
    private long maxWorkId;
    //maxUploadFailTime - 最大上报时间戳失败时间（默认为1小时）
    private long maxUploadFailTime = 1000*60*60*1;
    //每2分钟检测一次所有workId上报情况
    private long checkWorkIdStatusInterval = 2;
    // 当长时间与zookeeper失去连接时，会将shouldGenerate设置为false暂停id生成服务
    private boolean shouldGenerate = true;

    public RecyclableZookeeperHolder(String ip, Integer port, String zkAddress) {
        this.ip = ip;
        this.port = String.valueOf(port);
        this.zkAddress = zkAddress;
        this.maxWorkId = 1024;
    }

    public RecyclableZookeeperHolder(String ip, String port, String zkAddress, long maxWorkId) {
        this.ip = ip;
        this.port = port;
        this.zkAddress = zkAddress;
        this.maxWorkId = maxWorkId;
    }

    /**
     * 1.检查zookeeper上是否有/notuse/路径,也就是未使用的workId池，
     * 没有就创建并初始化未使用的workId池。
     * 2.从未使用的workId池中取出一个可用workId，
     * 从未使用的workId中删除，在正在的workId池中新建该节点。
     * 3.创建定时任务定时上传该workId的时间戳，
     * 如果超过maxUploadFailTime后还没有上传时间戳成功就停止id生成服务。
     * 4.创建定时任务定时检查正在使用的workId池，
     * 将超过maxUploadFailTime还没更新的workId移除，并添加到未使用的workId池。
     */
    public boolean init() {
        try {
            CuratorFramework curator = createWithOptions(zkAddress, new RetryUntilElapsed(1000, 4), 10000, 6000);
            curator.start();
            Stat stat = curator.checkExists().forPath(PATH_RECYCLE_NOTUSE);
            if (stat == null) {
                initAllNode(curator);
            }
            
            //从未使用的workId池中取一个可用节点
            List<String> keys = curator.getChildren().forPath(PATH_RECYCLE_NOTUSE);
            if (keys == null || keys.size() == 0) {
                LOGGER.error("no userful Node in "+ PATH_RECYCLE_NOTUSE);
                throw new NoUserfulWorkIdException("no userful Node in /notuse");
            }
            String firstUsefulNodeName = keys.get(0);
            byte[] bytes = curator.getData().forPath(PATH_RECYCLE_NOTUSE + "/" + firstUsefulNodeName);
            Endpoint endPoint = deBuildData(new String(bytes));
            if (endPoint != null && endPoint.workId != null) {
                //这个workId之前的生成过id的timeStamp比现在的时间大，抛出异常
                if (endPoint.lastUsedTimestamp != null && endPoint.lastUsedTimestamp > System.currentTimeMillis() && endPoint.workId != null) {
                    throw new CheckLastTimeException("init timestamp check error,forever node timestamp gt this node time");
                }
                workerID = endPoint.workId;
                //将当前选取节点从未使用的workId池中删除
                curator.delete().forPath(PATH_RECYCLE_NOTUSE + "/" + firstUsefulNodeName);
                //在正在使用的workId池(也就是/inuse/路径下)创建节点
                curator.create().
                        creatingParentsIfNeeded().
                        withMode(CreateMode.PERSISTENT).forPath(PATH_RECYCLE_INUSE + "/" + firstUsefulNodeName, bytes);
                //设置定时任务定时上报时间戳，更新这个节点中的时间戳信息及检查正在使用的workId池，将已经没有在使用的workId移除
                doService(curator,PATH_RECYCLE_INUSE + "/" + firstUsefulNodeName);
                LOGGER.info("[Old NODE]find forever node have this endpoint ip-{} port-{} workid-{} childnode and start SUCCESS", ip, port, workerID);
                return true;
            } else {
                throw new NoUserfulWorkIdException("no userful Node in /notuse");
            }
        } catch (Exception e) {
            //连接zookeeper失败，抛出异常
            LOGGER.error("Start node ERROR {}", e);
            throw new ZookeeperConnectFailException("Connect Zookeeper failed");
        }
    }

    /**
     * 第一次启动，会在未使用的workId池(也就是/notuse/路径下)创建1024个永久顺序节点，
     * 格式类似于这种workId:2
     * @param curator
     * @throws Exception
     */
    private void initAllNode(CuratorFramework curator) throws Exception {
        try {
            for (Integer i = 0; i <= maxWorkId; i++) {
                Endpoint endpoint = new Endpoint(i);
                ObjectMapper mapper = new ObjectMapper();
                String json = mapper.writeValueAsString(endpoint);
                curator.create().
                        creatingParentsIfNeeded().
                        withMode(CreateMode.PERSISTENT).
                        forPath(PATH_RECYCLE_NOTUSE + "/" + "workId:"+ i , json.getBytes());
            }
        } catch (Exception e) {
            LOGGER.error("create node error msg {} ", e.getMessage());
            throw e;
        }
    }

    /**
     * 1.设置定时任务定时上报时间戳，更新这个节点中的时间戳信息
     * 2.设置定时任务定时检查正在使用的workId池，将已经没有在使用的workId移除
     * @param curator
     * @param path 节点的路径
     */
    private void doService(CuratorFramework curator, String path) {
        scheduledUploadData(curator, path);
        scheduledCheckWorkIdUploadStatus(curator);
    }

    /**
     *  设置定时任务定时，定时调用updateNewData方法更新节点信息（每3s调用一次）
     * @param curator
     * @param path 节点的路径
     */
    private void scheduledUploadData(final CuratorFramework curator, final String path) {
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "schedule-upload-time");
                thread.setDaemon(true);
                return thread;
            }
        }).scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                updateNewData(curator, path);
            }
        }, 1L, 3L, TimeUnit.SECONDS);//每3s上报数据

    }

    /**
     *  更新这个节点中的时间戳信息，
     *  如果更新失败，并且已经超过maxUploadFailTime时候，那么这个节点会停止id生成服务，
     *  因为其他服务器会把这个workId从此workId从正在使用的workId池移除，
     *  并在未使用workId池中新增这个节点，继续使用可能会导致重复，所以停止id生成服务。
     * @param curator
     * @param path 节点的路径
     */
    private void updateNewData(CuratorFramework curator, String path) {
        try {
            if (lastUpdateTime!=null && System.currentTimeMillis() < lastUpdateTime) {
                return;
            }
            curator.setData().forPath(path, buildData().getBytes());
            lastUpdateTime = System.currentTimeMillis();
        } catch (Exception e) {
            LOGGER.info("update init data error path is {} error is {}", path, e);
            if (lastUpdateTime != null && System.currentTimeMillis() - lastUpdateTime > maxUploadFailTime){
                this.shouldGenerate = false;
                throw new ZookeeperConnectFailException("lost connect to zookeeper over maxUploadFailTime");
            }
        }
    }

    /**
     * 设置定时任务定时检查正在使用的workId池（默认每个服务器每2分钟调用一次）
     * @param curator
     */
    private void scheduledCheckWorkIdUploadStatus(final CuratorFramework curator) {
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "schedule-checkWorkId-time");
                thread.setDaemon(true);
                return thread;
            }
        }).scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                checkWorkIdUploadStatus(curator);
            }
        }, checkWorkIdStatusInterval, checkWorkIdStatusInterval, TimeUnit.MINUTES);//每2分钟上报数据

    }

    /**
     * 超过maxUploadFailTime时候后，使用某个workId的服务器还没有在zookeeper上更新时间戳信息
     * 使用其他workId的服务器会将此workId从正在使用的workId池移除，并在未使用workId池中新增这个节点，以供循环使用。
     * @param curator
     */
    private void checkWorkIdUploadStatus(CuratorFramework curator) {
        try {
            //从正在使用的workId池下取出所有节点进行遍历判断
            List<String> keys = curator.getChildren().forPath(PATH_RECYCLE_INUSE);
            for (int i = 0; i < keys.size(); i++) {
                String nodeName = keys.get(i);
                byte[] bytes = curator.getData().forPath(PATH_RECYCLE_INUSE + "/" + nodeName);
                Endpoint endPoint = deBuildData(new String(bytes));
                //当某个workId的已经超过1小时+4分钟还没有更新的话，就进行从正在使用的workId池移除，放入未使用workId池
                if (endPoint!=null && endPoint.getLastUsedTimestamp() !=null
                        && System.currentTimeMillis() - endPoint.getLastUsedTimestamp()
                        > maxUploadFailTime+2*checkWorkIdStatusInterval) {
                    curator.delete().forPath(PATH_RECYCLE_INUSE + "/" + nodeName);
                    //在正在使用的workId池中下创建节点,将这个workId的时间戳更新到当前时间
                    endPoint.setLastUsedTimestamp(System.currentTimeMillis());
                    System.currentTimeMillis();
                    curator.create().
                            withMode(CreateMode.PERSISTENT).forPath(PATH_RECYCLE_NOTUSE + "/" + nodeName, bytes);
                    LOGGER.info("remove workID {} from inuse to notuse success", nodeName);
                }
            }
        } catch (Exception e) {
            LOGGER.error("check workID status error path is {} error is {}", PATH_RECYCLE_INUSE, e);
        }
    }

    /**
     * 是否应该停止id生成服务
     * 当长时间与zookeeper失去连接时，
     * 会将shouldGenerate设置为false暂停id生成服务
     */
    public boolean getShouldGenerateContinue() {
        return this.shouldGenerate;
    }

    /**
     * 构建需要上传的数据，包含workerID，ip，port，当前时间戳
     * @return
     */
    private String buildData() throws JsonProcessingException {
        Endpoint endpoint = new Endpoint(workerID, ip, port, System.currentTimeMillis());
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(endpoint);
        return json;
    }

    /**
     * 解析节点上存储的json数据，解析成Endpoint对象
     * @return Endpoint
     */
    private Endpoint deBuildData(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Endpoint endpoint = mapper.readValue(json, Endpoint.class);
        return endpoint;
    }

    /**
     * 与zookeeper建立连接
     * @return Endpoint
     */
    private CuratorFramework createWithOptions(String zkAddress, RetryPolicy retryPolicy, int connectionTimeoutMs, int sessionTimeoutMs) {
        return CuratorFrameworkFactory.builder().connectString(zkAddress)
                .retryPolicy(retryPolicy)
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .build();
    }

    /**
     * 上报数据结构 
     * workId
     * lastUsedIp 上次使用这个workId的服务器ip
     * lastUsedPort 上次使用这个workId的端口
     * lastUsedTimestamp 上次使用这个workId生成id的最大时间戳
     */
    static class Endpoint {

        private Integer workId;
        private String lastUsedIp;
        private String lastUsedPort;
        private Long lastUsedTimestamp;

        public Endpoint() {

        }
        public Endpoint(Integer workId) {
            this.workId = workId;
        }

        public Endpoint(Integer workId, String lastUsedIp, String lastUsedPort, long lastUsedTimestamp) {
            this.workId = workId;
            this.lastUsedIp = lastUsedIp;
            this.lastUsedPort = lastUsedPort;
            this.lastUsedTimestamp = lastUsedTimestamp;
        }

        public Integer getWorkId() {
            return workId;
        }

        public void setWorkId(Integer workId) {
            this.workId = workId;
        }

        public String getLastUsedIp() {
            return lastUsedIp;
        }

        public void setLastUsedIp(String lastUsedIp) {
            this.lastUsedIp = lastUsedIp;
        }

        public String getLastUsedPort() {
            return lastUsedPort;
        }

        public void setLastUsedPort(String lastUsedPort) {
            this.lastUsedPort = lastUsedPort;
        }

        public Long getLastUsedTimestamp() {
            return lastUsedTimestamp;
        }

        public void setLastUsedTimestamp(Long lastUsedTimestamp) {
            this.lastUsedTimestamp = lastUsedTimestamp;
        }

    }

    public int getWorkerID() {
        return workerID;
    }

    public void setWorkerID(int workerID) {
        this.workerID = workerID;
    }

}
