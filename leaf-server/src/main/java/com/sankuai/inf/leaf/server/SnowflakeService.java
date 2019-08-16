package com.sankuai.inf.leaf.server;

import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.ZeroIDGen;
import com.sankuai.inf.leaf.server.exception.InitException;
import com.sankuai.inf.leaf.snowflake.SnowflakeIDGenImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service("SnowflakeService")
public class SnowflakeService {
    private Logger logger = LoggerFactory.getLogger(SnowflakeService.class);
    @Value("${leaf.name}")
    private String leafName;
    @Value("${leaf.snowflake.enable:true}")
    private Boolean leafSnowflakeEnable;
    @Value("${leaf.snowflake.zk.address}")
    private String leafSnowflakeZkAddress;
    @Value("${leaf.snowflake.zk.port}")
    private Integer leafSnowflakePort;

    IDGen idGen;

    @PostConstruct
    public void init() throws InitException {
        boolean flag = leafSnowflakeEnable;
        if (flag) {
            String zkAddress = leafSnowflakeZkAddress;
            int port = leafSnowflakePort;
            idGen = new SnowflakeIDGenImpl(zkAddress, port,leafName);
            if(idGen.init()) {
                logger.info("Snowflake Service Init Successfully");
            } else {
                throw new InitException("Snowflake Service Init Fail");
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
