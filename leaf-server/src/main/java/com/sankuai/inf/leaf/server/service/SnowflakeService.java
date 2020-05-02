package com.sankuai.inf.leaf.server.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.PropertyFactory;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Utils;
import com.sankuai.inf.leaf.common.ZeroIDGen;
import com.sankuai.inf.leaf.segment.dao.WorkerIdAllocDao;
import com.sankuai.inf.leaf.segment.dao.impl.WorkerIdAllocDaoImpl;
import com.sankuai.inf.leaf.server.Constants;
import com.sankuai.inf.leaf.server.exception.InitException;
import com.sankuai.inf.leaf.snowflake.SnowflakeIDGenImpl;
import com.sankuai.inf.leaf.snowflake.SnowflakeLocalHolder;
import com.sankuai.inf.leaf.snowflake.SnowflakeMySQLHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

@Service("SnowflakeService")
public class SnowflakeService {
    private Logger logger = LoggerFactory.getLogger(SnowflakeService.class);

    private IDGen idGen;

    public SnowflakeService() throws SQLException, InitException, IOException {
        Properties properties = PropertyFactory.getProperties();
        boolean flag = Boolean.parseBoolean(properties.getProperty(Constants.LEAF_SNOWFLAKE_ENABLE, "true"));
        String mode = properties.getProperty(Constants.LEAF_SNOWFLAKE_MODE, "zk");
        //当前leaf服务的端口
        int    port      = Integer.parseInt(properties.getProperty(Constants.LEAF_SNOWFLAKE_PORT));

        if (flag) {
            if (mode.equals("zk")) {//注册中心为zk
                String zkAddress = properties.getProperty(Constants.LEAF_SNOWFLAKE_ZK_ADDRESS);
                idGen = new SnowflakeIDGenImpl(zkAddress, port);
                if (idGen.init()) {
                    logger.info("Snowflake Service Init Successfully in mode " + mode);
                } else {
                    throw new InitException("Snowflake Service Init Fail");
                }
            } else if (mode.equals("mysql")) {
                DruidDataSource dataSource = new DruidDataSource();
                dataSource.setUrl(properties.getProperty(Constants.LEAF_JDBC_URL));
                dataSource.setUsername(properties.getProperty(Constants.LEAF_JDBC_USERNAME));
                dataSource.setPassword(properties.getProperty(Constants.LEAF_JDBC_PASSWORD));
                dataSource.init();
                // Config Dao
                WorkerIdAllocDao dao = new WorkerIdAllocDaoImpl(dataSource);

                SnowflakeMySQLHolder holder = new SnowflakeMySQLHolder(Utils.getIp(), port, dao);
                idGen  = new SnowflakeIDGenImpl(holder);
                if (idGen.init()) {
                    logger.info("Snowflake Service Init Successfully in mode " + mode);
                } else {
                    throw new InitException("Snowflake Service Init Fail");
                }
            } else if (mode.equals("local")) {
                ObjectMapper mapper = new ObjectMapper();
                String  workIdMapString = PropertyFactory.getProperties().getProperty(Constants.LEAF_SNOWFLAKE_LOCAL_WORKIDMAP);
                HashMap<String,Integer> workIdMap = mapper.readValue(workIdMapString, HashMap.class);
                SnowflakeLocalHolder holder = new SnowflakeLocalHolder(Utils.getIp(), port,workIdMap);

                idGen  = new SnowflakeIDGenImpl(holder);
                if (idGen.init()) {
                    logger.info("Snowflake Service Init Successfully in mode " + mode);
                } else {
                    throw new InitException("Snowflake Service Init Fail");
                }
            }
        } else {
            idGen = new ZeroIDGen();
            logger.info("Zero ID Gen Service Init Successfully");
        }
    }

    public Result getId(String key) {
        return idGen.get(key);
    }
}
