package com.sankuai.inf.leaf.snowflake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankuai.inf.leaf.snowflake.exception.CheckLastTimeException;
import org.apache.commons.io.FileUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.sankuai.inf.leaf.common.*;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.zookeeper.CreateMode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * @author mickle
 */
public class SnowflakeZookeeperHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeZookeeperHolder.class);
    /**
     *  保存自身的key  ip:port-000000001
     */
    private String zkAddressNode = null;
    /**
     * 保存自身的key ip:port
     */
    private String listenAddress = null;
    /**
     * 机器号
     */
    private int workerId;
    /**
     * zk路径前缀
     */
    private static final String PREFIX_ZK_PATH = "/snowflake/" + PropertyFactory.getProperties().getProperty("leaf.name");
    private static final String PROP_PATH = System.getProperty("java.io.tmpdir") + File.separator + PropertyFactory.getProperties().getProperty("leaf.name") + "/leafconf/{port}/workerID.properties";
    /**
     * 保存所有数据持久的节点
     */
    private static final String PATH_FOREVER = PREFIX_ZK_PATH + "/forever";
    /**
     * 本机ip
     */
    private final String ip;
    /**
     * 端口号
     */
    private final String port;
    /**
     * zk连接字符串
     */
    private final String connectionString;
    /**
     * 上次上报时间
     */
    private long lastUpdateTime;

    public SnowflakeZookeeperHolder(String ip, String port, String connectionString) {
        this.ip = ip;
        this.port = port;
        this.listenAddress = ip + ":" + port;
        this.connectionString = connectionString;
    }

    public boolean init() {
        try {
            CuratorFramework curator = createWithOptions(connectionString, new RetryUntilElapsed(1000, 4), 10000, 6000);
            curator.start();
            Stat stat = curator.checkExists().forPath(PATH_FOREVER);
            if (stat == null) {
                //不存在根节点,机器第一次启动,创建/snowflake/ip:port-000000000,并上传数据
                zkAddressNode = createNode(curator);
                //worker id 默认是0
                updateLocalWorkerID(workerId);
                //定时上报本机时间给forever节点
                scheduledUploadData(curator, zkAddressNode);
                return true;
            } else {
                // ip:port->00001
                Map<String, Integer> nodeMap = Maps.newHashMap();
                // ip:port->(ip port - 000001)
                Map<String, String> realNode = Maps.newHashMap();
                // 存在根节点,先检查是否有属于自己的根节点
                List<String> keys = curator.getChildren().forPath(PATH_FOREVER);
                for (String key : keys) {
                    String[] nodeKey = key.split("-");
                    realNode.put(nodeKey[0], key);
                    nodeMap.put(nodeKey[0], Integer.parseInt(nodeKey[1]));
                }
                // 获取机器👌
                Integer workerid = nodeMap.get(listenAddress);
                if (workerid != null) {
                    // 有自己的节点,zkAddressNode=ip:port
                    zkAddressNode = PATH_FOREVER + "/" + realNode.get(listenAddress);
                    // 启动worder时使用会使用
                    workerId = workerid;
                    if (!checkInitTimeStamp(curator, zkAddressNode)) {
                        throw new CheckLastTimeException("init timestamp check error,forever node timestamp gt this node time");
                    }
                    //定时上报本机时间给forever节点
                    doService(curator);
                    // 本地缓存机器号
                    updateLocalWorkerID(workerId);
                    LOGGER.info("[Old NODE]find forever node have this endpoint ip-{} port-{} workid-{} childnode and start SUCCESS", ip, port, workerId);
                } else {
                    //表示新启动的节点,创建持久节点 ,不用check时间
                    String newNode = createNode(curator);
                    zkAddressNode = newNode;
                    String[] nodeKey = newNode.split("-");
                    workerId = Integer.parseInt(nodeKey[1]);
                    //定时上报本机时间给forever节点
                    doService(curator);
                    // 本地缓存机器号
                    updateLocalWorkerID(workerId);
                    LOGGER.info("[New NODE]can not find node on forever node that endpoint ip-{} port-{} workid-{},create own node on forever node and start SUCCESS ", ip, port, workerId);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Start node ERROR {0}", e);
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(new File(PROP_PATH.replace("{port}", port + ""))));
                workerId = Integer.parseInt(properties.getProperty("workerID"));
                LOGGER.warn("START FAILED ,use local node file properties workerID-{}", workerId);
            } catch (Exception e1) {
                LOGGER.error("Read file error ", e1);
                return false;
            }
        }
        return true;
    }

    private void doService(CuratorFramework curator) {
        // /snowflake_forever/ip:port-000000001
        scheduledUploadData(curator, zkAddressNode);
    }

    /**
     * 定时上报数据至zk
     * @param curator
     * @param zkAddressNode
     */
    private void scheduledUploadData(final CuratorFramework curator, final String zkAddressNode) {
        // 每3s上报数据
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
                updateNewData(curator, zkAddressNode);
            }
        }, 1L, 3L, TimeUnit.SECONDS);

    }

    /**
     * 检查初始化时间戳
     * 该节点当前的时间不能小于最后一次上报的时间
     * @param curator
     * @param zkAddressNode
     * @return
     * @throws Exception
     */
    private boolean checkInitTimeStamp(CuratorFramework curator, String zkAddressNode) throws Exception {
        byte[] bytes = curator.getData().forPath(zkAddressNode);
        Endpoint endPoint = deBuildData(new String(bytes));
        //该节点当前的时间不能小于最后一次上报的时间
        return endPoint.getTimestamp() <= System.currentTimeMillis();
    }

    /**
     * 创建持久顺序节点 ,并把节点数据放入 value
     * 自动在路径后加顺序编号
     *
     * @param curator
     * @return            节点
     * @throws Exception
     */
    private String createNode(CuratorFramework curator) throws Exception {
        try {
            return curator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(PATH_FOREVER + "/" + listenAddress + "-", buildData().getBytes());
        } catch (Exception e) {
            LOGGER.error("create node error msg {} ", e.getMessage());
            throw e;
        }
    }

    /**
     * 修改节点值
     * @param curator
     * @param path
     */
    private void updateNewData(CuratorFramework curator, String path) {
        try {
            if (System.currentTimeMillis() < lastUpdateTime) {
                return;
            }
            curator.setData().forPath(path, buildData().getBytes());
            lastUpdateTime = System.currentTimeMillis();
        } catch (Exception e) {
            LOGGER.info("update init data error path is {} error is {}", path, e);
        }
    }

    /**
     * 构建需要上传的数据
     *
     * @return          Endpoint
     */
    private String buildData() throws JsonProcessingException {
        Endpoint endpoint = new Endpoint(ip, port, System.currentTimeMillis());
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(endpoint);
    }

    /**
     * 解析zk数据
     * @param json            字符数据
     * @return
     * @throws IOException
     */
    private Endpoint deBuildData(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, Endpoint.class);
    }

    /**
     * 在节点文件系统上缓存一个workId值,zk失效,机器重启时保证能够正常启动
     *
     * @param workerId
     */
    private void updateLocalWorkerID(int workerId) {
        File leafConfFile = new File(PROP_PATH.replace("{port}", port));
        boolean exists = leafConfFile.exists();
        LOGGER.info("file exists status is {}", exists);
        if (exists) {
            try {
                FileUtils.writeStringToFile(leafConfFile, "workerID=" + workerId, false);
                LOGGER.info("update file cache workerID is {}", workerId);
            } catch (IOException e) {
                LOGGER.error("update file cache error ", e);
            }
        } else {
            //不存在文件,父目录页肯定不存在
            try {
                boolean mkdirs = leafConfFile.getParentFile().mkdirs();
                LOGGER.info("init local file cache create parent dis status is {}, worker id is {}", mkdirs, workerId);
                if (mkdirs) {
                    if (leafConfFile.createNewFile()) {
                        FileUtils.writeStringToFile(leafConfFile, "workerId=" + workerId, false);
                        LOGGER.info("local file cache workerId is {}", workerId);
                    }
                } else {
                    LOGGER.warn("create parent dir error===");
                }
            } catch (IOException e) {
                LOGGER.warn("create workerId conf file error", e);
            }
        }
    }

    private CuratorFramework createWithOptions(String connectionString, RetryPolicy retryPolicy, int connectionTimeoutMs, int sessionTimeoutMs) {
        return CuratorFrameworkFactory.builder().connectString(connectionString)
                .retryPolicy(retryPolicy)
                .connectionTimeoutMs(connectionTimeoutMs)
                .sessionTimeoutMs(sessionTimeoutMs)
                .build();
    }

    /**
     * 上报数据结构
     */
    static class Endpoint {
        private String ip;
        private String port;
        private long timestamp;

        public Endpoint() {
        }

        public Endpoint(String ip, String port, long timestamp) {
            this.ip = ip;
            this.port = port;
            this.timestamp = timestamp;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getPort() {
            return port;
        }

        public void setPort(String port) {
            this.port = port;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    public String getZkAddressNode() {
        return zkAddressNode;
    }

    public void setZkAddressNode(String zkAddressNode) {
        this.zkAddressNode = zkAddressNode;
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }

    public int getWorkerId() {
        return workerId;
    }

    public void setWorkerId(int workerId) {
        this.workerId = workerId;
    }

}
