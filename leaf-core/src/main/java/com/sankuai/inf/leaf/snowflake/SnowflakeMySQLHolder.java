package com.sankuai.inf.leaf.snowflake;

import com.sankuai.inf.leaf.common.PropertyFactory;
import com.sankuai.inf.leaf.segment.dao.WorkerIdAllocDao;
import com.sankuai.inf.leaf.segment.model.LeafWorkerIdAlloc;
import com.sankuai.inf.leaf.snowflake.exception.CheckLastTimeException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SnowflakeMySQLHolder implements SnowflakeHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeMySQLHolder.class);

    private String listenAddress = null;//保存自身的key ip:port
    private int workerID;
    private static final String PROP_PATH = System.getProperty("java.io.tmpdir") + File.separator + PropertyFactory.getProperties().getProperty("leaf.name") + "/leafconf/{port}/workerID.properties";
    private String ip;
    private String port;
    private long lastUpdateTime;

    private WorkerIdAllocDao dao;

    public SnowflakeMySQLHolder(String ip, Integer port, WorkerIdAllocDao dao) {
        this.ip = ip;
        this.port = port.toString();
        this.dao = dao;
        this.listenAddress = ip + ":" + port;
    }

    public boolean init() {
        try {
            LeafWorkerIdAlloc workIdAlloc = dao.getOrCreateLeafWorkerId(ip,port,listenAddress);
            if (workIdAlloc.getMaxTimestamp()>System.currentTimeMillis()) {
                throw new CheckLastTimeException("init timestamp check error,db workid  node maxTimestamp gt this node time");
            }
            this.workerID = workIdAlloc.getId();
            //更新workID到本地缓存文件
            updateLocalWorkerID(workerID);
            ScheduledUploadData();
        } catch (Exception e) {
            LOGGER.error("Start node ERROR {}", e);
            try {
                Properties properties = new Properties();
                properties.load(new FileInputStream(new File(PROP_PATH.replace("{port}", port + ""))));
                workerID = Integer.valueOf(properties.getProperty("workerID"));
                LOGGER.warn("START FAILED ,use local node file properties workerID-{}", workerID);
            } catch (Exception e1) {
                LOGGER.error("Read file error ", e1);
                return false;
            }
        }
        return true;
    }


    private void ScheduledUploadData() {
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
                updateNewData();
            }
        }, 1L, 3L, TimeUnit.SECONDS);//每3s上报数据

    }

    private void updateNewData() {
        try {
            Long current = System.currentTimeMillis();
            if (current < lastUpdateTime) {
                return;
            }
            dao.updateMaxTimestamp(workerID, current);
            lastUpdateTime = System.currentTimeMillis();
        } catch (Exception e) {
            LOGGER.info("update maxTimestamp to db error, workerID is {} error is {}", workerID, e);
        }
    }

    /**
     * 在节点文件系统上缓存一个workid值,zk失效,机器重启时保证能够正常启动
     * @param workerID
     */
    private void updateLocalWorkerID(int workerID) {
        File leafConfFile = new File(PROP_PATH.replace("{port}", port));
        boolean exists = leafConfFile.exists();
        LOGGER.info("file exists status is {}", exists);
        if (exists) {
            try {
                FileUtils.writeStringToFile(leafConfFile, "workerID=" + workerID, false);
                LOGGER.info("update file cache workerID is {}", workerID);
            } catch (IOException e) {
                LOGGER.error("update file cache error ", e);
            }
        } else {
            //不存在文件,父目录页肯定不存在
            try {
                boolean mkdirs = leafConfFile.getParentFile().mkdirs();
                LOGGER.info("init local file cache create parent dis status is {}, worker id is {}", mkdirs, workerID);
                if (mkdirs) {
                    if (leafConfFile.createNewFile()) {
                        FileUtils.writeStringToFile(leafConfFile, "workerID=" + workerID, false);
                        LOGGER.info("local file cache workerID is {}", workerID);
                    }
                } else {
                    LOGGER.warn("create parent dir error===");
                }
            } catch (IOException e) {
                LOGGER.warn("craete workerID conf file error", e);
            }
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

    public WorkerIdAllocDao getDao() {
        return dao;
    }

    public void setDao(WorkerIdAllocDao dao) {
        this.dao = dao;
    }
}
