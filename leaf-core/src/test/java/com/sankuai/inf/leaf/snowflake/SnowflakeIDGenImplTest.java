package com.sankuai.inf.leaf.snowflake;

import com.alibaba.druid.pool.DruidDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.PropertyFactory;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Utils;
import com.sankuai.inf.leaf.segment.dao.WorkerIdAllocDao;
import com.sankuai.inf.leaf.segment.dao.impl.WorkerIdAllocDaoImpl;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Properties;

public class SnowflakeIDGenImplTest {

    @Test
    public void testGetId() {
        Properties properties = PropertyFactory.getProperties();
        IDGen idGen = new SnowflakeIDGenImpl(properties.getProperty("leaf.zk.list"), 8080);
        for (int i = 1; i < 1000; ++i) {
            Result r = idGen.get("a");
            System.out.println(r);
        }
    }

    @Test
    public void testGetIdInLocalHolder() throws IOException {
        ObjectMapper mapper          = new ObjectMapper();
        String                  workIdMapString = PropertyFactory.getProperties().getProperty("leaf.snowflake.local.workIdMap");
        HashMap<String,Integer> workIdMap       = mapper.readValue(workIdMapString, HashMap.class);
        SnowflakeLocalHolder holder = new SnowflakeLocalHolder(Utils.getIp(),8080, workIdMap);
        IDGen idGen = new SnowflakeIDGenImpl(holder);
        for (int i = 1; i < 1000; ++i) {
            Result r = idGen.get("a");
            System.out.println(r);
        }
    }

    @Test
    public void testGetIdInMySQLHolder() throws SQLException {
        // Load Db Config
        Properties properties = PropertyFactory.getProperties();

        // Config dataSource
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(properties.getProperty("jdbc.url"));
        dataSource.setUsername(properties.getProperty("jdbc.username"));
        dataSource.setPassword(properties.getProperty("jdbc.password"));
        dataSource.init();

        // Config Dao
        WorkerIdAllocDao     dao    = new WorkerIdAllocDaoImpl(dataSource);
        SnowflakeMySQLHolder holder = new SnowflakeMySQLHolder(Utils.getIp(), 8082, dao);
        IDGen                idGen  = new SnowflakeIDGenImpl(holder);
    }


    public void testGetIdInRecyclableMode() {
        Properties properties = PropertyFactory.getProperties();
        RecyclableZookeeperHolder holder = new RecyclableZookeeperHolder(Utils.getIp(),8080,properties.getProperty("leaf.zk.list"));

        IDGen idGen = new SnowflakeIDGenImpl(holder);
        for (int i = 1; i < 1000; ++i) {
            Result r = idGen.get("a");
            System.out.println(r);
        }
    }
}
