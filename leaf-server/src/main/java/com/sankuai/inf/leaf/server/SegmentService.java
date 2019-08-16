package com.sankuai.inf.leaf.server;

import com.alibaba.druid.pool.DruidDataSource;
import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.ZeroIDGen;
import com.sankuai.inf.leaf.segment.SegmentIDGenImpl;
import com.sankuai.inf.leaf.segment.dao.IDAllocDao;
import com.sankuai.inf.leaf.segment.dao.impl.IDAllocDaoImpl;
import com.sankuai.inf.leaf.server.exception.InitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.SQLException;

@Service("SegmentService")
public class SegmentService {
    private Logger logger = LoggerFactory.getLogger(SegmentService.class);
    @Value("${leaf.segment.enable:false}")
    private Boolean leafSegmentEnable;
    @Value("${leaf.jdbc.url:''}")
    private String leafJdbcUrl;
    @Value("${leaf.jdbc.username:''}")
    private String leafJdbcUsername;
    @Value("${leaf.jdbc.password:''}")
    private String leafJdbcPassword;

    IDGen idGen;
    DruidDataSource dataSource;

    @PostConstruct
    public void init() throws SQLException, InitException {
        boolean flag = leafSegmentEnable;
        if (flag) {


            // Config dataSource
            dataSource = new DruidDataSource();
            dataSource.setUrl(leafJdbcUrl);
            dataSource.setUsername(leafJdbcUsername);
            dataSource.setPassword(leafJdbcPassword);
            dataSource.init();

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
