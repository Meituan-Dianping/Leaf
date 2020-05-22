package com.sankuai.inf.leaf.snowflake;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankuai.inf.leaf.common.PropertyFactory;
import com.sankuai.inf.leaf.snowflake.exception.CheckLastTimeException;
import com.sankuai.inf.leaf.snowflake.exception.LocalWorkIdNotFoundException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SnowflakeLocalHolder implements SnowflakeHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeLocalHolder.class);

    private String listenAddress = null;//保存自身的key ip:port
    private int workerID;
    private String ip;
    private String port;
    private String localWorkIdPath;
    private long lastUpdateTime;
    private HashMap<String, Integer> workIdMap;
    private ObjectMapper mapper = new ObjectMapper();

    public SnowflakeLocalHolder(String ip, int port, HashMap<String, Integer> workIdMap) {
        this.ip = ip;
        this.port = String.valueOf(port);
        this.listenAddress = ip + ":" + port;
        String PROP_PATH = System.getProperty("java.io.tmpdir") + File.separator + PropertyFactory.getProperties().getProperty("leaf.name") + "/leafconf/{port}/workerID.json";
        this.localWorkIdPath = PROP_PATH.replace("{port}", String.valueOf(port));
        LOGGER.info(" local workerID conf file path is " + this.localWorkIdPath);
        this.workIdMap = workIdMap;
    }

    public boolean init() {
        File file = new File(localWorkIdPath);
        if (file.exists() == false) {
            //本地缓存不存在文件,机器第一次启动,直接去workIdMap配置中取ip:port对应的workId
            if (workIdMap != null && workIdMap.get(listenAddress) != null) {
                this.workerID = workIdMap.get(listenAddress);
            } else {
                throw new LocalWorkIdNotFoundException("cannot find leaf.snowflake.local.workIdMap in leaf.properties, listenAddress is " + listenAddress);
            }
            updateLocalTimestamp();
            //定时更新本地缓存的时间戳
            ScheduledUpdateLocalTimestamp();
            return true;
        } else {//去本地缓存读取workId信息
            String endpointString = null;
            try {
                endpointString = FileUtils.readFileToString(file);
                if (endpointString != null && endpointString.length()>0) {
                    Endpoint endpoint = deBuildData(endpointString);
                    if (endpoint==null || endpoint.getWorkId() == null) {
                        throw new LocalWorkIdNotFoundException("parse local workerID error, workerID is null listenAddress is " + listenAddress);
                    } else if (endpoint.getTimestamp()!=null
                            && endpoint.getTimestamp() > System.currentTimeMillis()){
                        throw new CheckLastTimeException("init timestamp check error, local workID file timestamp gt this node time" + listenAddress);
                    } else {
                        workerID = endpoint.getWorkId();
                        //定时更新本地缓存的时间戳
                        ScheduledUpdateLocalTimestamp();
                        return true;
                    }
                } else {
                    throw new LocalWorkIdNotFoundException("parse local workerID conf file error, listenAddress is " + listenAddress);
                }
            } catch (IOException ioe) {
                LOGGER.warn("load local workerID conf file error {}", ioe);
                throw new LocalWorkIdNotFoundException("load local workerID conf file error, listenAddress is " + listenAddress);
            }
        }
    }

    private void ScheduledUpdateLocalTimestamp() {
        Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "schedule-update-time");
                thread.setDaemon(true);
                return thread;
            }
        }).scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                updateLocalTimestamp();
            }
        }, 1L, 3L, TimeUnit.SECONDS);//每3s上报数据
    }

    /**
     * 在节点文件系统上缓存当前Leaf服务的ip，port，workId，timestamp最大时间戳
     *
     */
    private void updateLocalTimestamp() {
        Long timeStamp = System.currentTimeMillis();
        if (timeStamp < lastUpdateTime) {
            return;
        }
        String json = buildData(timeStamp);
        File leafConfFile = new File(localWorkIdPath);
        boolean exists = leafConfFile.exists();
        LOGGER.info("file {} exists status is {}", localWorkIdPath, exists);
        if (exists) {
            try {
                FileUtils.writeStringToFile(leafConfFile, json, false);
                lastUpdateTime = System.currentTimeMillis();
                LOGGER.info("update file cache endPoint is {}", json);
            } catch (IOException e) {
                LOGGER.error("update file cache error ", e);
            }
        } else {
            //不存在文件,父目录页肯定不存在
            try {
                boolean mkdirs = leafConfFile.getParentFile().mkdirs();
                LOGGER.info("init local file cache create parent dis status is {}, endpoint is {}", mkdirs, json);
                if (mkdirs) {
                    if (leafConfFile.createNewFile()) {
                        FileUtils.writeStringToFile(leafConfFile, json, false);
                        lastUpdateTime = System.currentTimeMillis();
                        LOGGER.info("local file cache endpoint is {}", json);
                    }
                } else {
                    LOGGER.warn("create parent dir error===");
                }
            } catch (IOException e) {
                LOGGER.warn("craete workerID conf file error", e);
            }
        }
    }

    /**
     * 构建需要上传的数据
     * @return
     */
    private String buildData(Long timeStamp) {
        Endpoint endpoint = new Endpoint(ip, port, workerID, timeStamp);
        String json = null;
        try {
            json = mapper.writeValueAsString(endpoint);
        } catch (JsonProcessingException e) {
            LOGGER.info(" endpoint to json error endpoint is {} error is {}", endpoint, e);
        }
        return json;
    }

    private Endpoint deBuildData(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Endpoint endpoint = mapper.readValue(json, Endpoint.class);
        return endpoint;
    }

    /**
     * 上报数据结构
     */
    static class Endpoint {
        private String ip;
        private String port;
        private Integer workId;
        private Long timestamp;


        public Endpoint() {
        }

        public Endpoint(String ip, String port, int workId, long timestamp) {
            this.ip = ip;
            this.port = port;
            this.workId = workId;
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

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public Integer getWorkId() {
            return workId;
        }

        public void setWorkId(Integer workId) {
            this.workId = workId;
        }

        @Override
        public String toString() {
            return "Endpoint{" +
                    "ip='" + ip + '\'' +
                    ", port='" + port + '\'' +
                    ", timestamp=" + timestamp +
                    ", workId=" + workId +
                    '}';
        }
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }

    public int getWorkerID() {
        return workerID;
    }

    public void setWorkerID(int workerID) {
        this.workerID = workerID;
    }

    public boolean getShouldGenerateContinue() {
        return true;
    }

}
