package com.sankuai.inf.leaf.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.google.common.base.Preconditions;
import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.exception.InitException;
import com.sankuai.inf.leaf.segment.DailySegmentIDGenImpl;
import com.sankuai.inf.leaf.segment.SegmentIDGenImpl;
import com.sankuai.inf.leaf.segment.dao.DailyIDAllocDao;
import com.sankuai.inf.leaf.segment.dao.IDAllocDao;
import com.sankuai.inf.leaf.segment.dao.impl.DailyIDAllocDaoImpl;
import com.sankuai.inf.leaf.segment.dao.impl.IDAllocDaoImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/***
 *
 *
 * @author <a href="mailto:1934849492@qq.com">Darian</a> 
 * @date 2021/7/1  21:30
 */
public class DailySegmentService {
    private Logger logger = LoggerFactory.getLogger(SegmentService.class);

    private IDGen idGen;
    private DruidDataSource dataSource;

    public DailySegmentService(String url, String username, String pwd, String driverClassName) throws SQLException, InitException {
        Preconditions.checkNotNull(url,"database url can not be null");
        Preconditions.checkNotNull(username,"username can not be null");
        Preconditions.checkNotNull(pwd,"password can not be null");
        Preconditions.checkNotNull(driverClassName,"password can not be null");
        // Config dataSource
        dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(pwd);
        dataSource.setDriverClassName(driverClassName);
        dataSource.init();

        // daily config dao
        DailyIDAllocDao dailyIDAllocDao = new DailyIDAllocDaoImpl(dataSource);

        // Config Dao
        IDAllocDao dao = new IDAllocDaoImpl(dataSource);


        // Config ID Gen
        idGen = new DailySegmentIDGenImpl();
        ((DailySegmentIDGenImpl) idGen).setDao(dao);
        ((DailySegmentIDGenImpl) idGen).setDailyIDAllocDao(dailyIDAllocDao);
        if (idGen.init()) {
            logger.info("Segment Service Init Successfully");
        } else {
            throw new InitException("Segment Service Init Fail");
        }
    }

    public Result getId(String key) {
        return idGen.get(key);
    }

    public DailySegmentIDGenImpl getIdGen() {
        if (idGen instanceof DailySegmentIDGenImpl) {
            return (DailySegmentIDGenImpl) idGen;
        }
        return null;
    }
}
