package com.sankuai.inf.leaf.server.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.PropertyFactory;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.ZeroIDGen;
import com.sankuai.inf.leaf.segment.SegmentIDGenImpl;
import com.sankuai.inf.leaf.segment.dao.IDAllocDao;
import com.sankuai.inf.leaf.segment.dao.impl.IDAllocDaoImpl;
import com.sankuai.inf.leaf.server.Constants;
import com.sankuai.inf.leaf.server.exception.InitException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

@Service("SegmentService")
public class SegmentService {
    private Logger logger = LoggerFactory.getLogger(SegmentService.class);

    private IDGen idGen;

    /*private DataSource getDataSource(Properties properties) throws SQLException {
        // Config dataSource
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(properties.getProperty(Constants.LEAF_JDBC_URL));
        dataSource.setUsername(properties.getProperty(Constants.LEAF_JDBC_USERNAME));
        dataSource.setPassword(properties.getProperty(Constants.LEAF_JDBC_PASSWORD));
        dataSource.init();
        return dataSource;
    }*/

    private DataSource getDataSource(Properties properties) throws SQLException {
        // Config dataSource
        HikariConfig configuration = new HikariConfig();
        configuration.setDriverClassName(properties.getProperty(Constants.LEAF_JDBC_DRIVE));
        configuration.setJdbcUrl(properties.getProperty(Constants.LEAF_JDBC_URL));
        configuration.setUsername(properties.getProperty(Constants.LEAF_JDBC_USERNAME));
        configuration.setPassword(properties.getProperty(Constants.LEAF_JDBC_PASSWORD));
        DataSource dataSource = new HikariDataSource(configuration);
        return dataSource;
    }

    public SegmentService() throws SQLException, InitException {
        Properties properties = PropertyFactory.getProperties();
        boolean flag = Boolean.parseBoolean(properties.getProperty(Constants.LEAF_SEGMENT_ENABLE, "true"));
        if (flag) {

            DataSource dataSource  = getDataSource(properties);

            // Config Dao
            IDAllocDao dao = new IDAllocDaoImpl(dataSource);

            // Config ID Gen
            idGen = new SegmentIDGenImpl();
            ((SegmentIDGenImpl) idGen).setDao(dao);
            if (idGen.init()) {
                logger.info("Segment Service Init Successfully");
            } else {
                throw new InitException("Segment Service Init Fail");
            }
        } else {
            idGen = new ZeroIDGen();
            logger.info("Zero ID Gen Service Init Successfully");
        }
    }

    public Result getId(String key) {
        return idGen.get(key);
    }

    public SegmentIDGenImpl getIdGen() {
        if (idGen instanceof SegmentIDGenImpl) {
            return (SegmentIDGenImpl) idGen;
        }
        return null;
    }
}
