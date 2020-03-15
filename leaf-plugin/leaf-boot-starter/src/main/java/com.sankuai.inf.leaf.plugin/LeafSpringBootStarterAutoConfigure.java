package com.sankuai.inf.leaf.plugin;

import com.sankuai.inf.leaf.exception.InitException;
import com.sankuai.inf.leaf.service.SegmentService;
import com.sankuai.inf.leaf.service.SnowflakeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhaodong.xzd (github.com/yaccc)
 * @date 2019/10/09
 * @since support springboot starter with dubbo and etc rpc
 */
@Configuration
@EnableConfigurationProperties(LeafSpringBootProperties.class)
public class LeafSpringBootStarterAutoConfigure {
    private Logger logger = LoggerFactory.getLogger(LeafSpringBootStarterAutoConfigure.class);
    @Autowired
    private LeafSpringBootProperties properties;

    @Bean
    public SegmentService initLeafSegmentStarter() throws Exception {
        if (properties != null && properties.getSegment() != null && properties.getSegment().isEnable()) {
            SegmentService segmentService = new SegmentService(properties.getSegment().getUrl(), properties.getSegment().getUsername(), properties.getSegment().getPassword());
            return segmentService;
        }
        logger.warn("init leaf segment ignore properties is {}", properties);
        return null;
    }

    @Bean
    public SnowflakeService initLeafSnowflakeStarter() throws InitException {
        if (properties != null && properties.getSnowflake() != null && properties.getSnowflake().isEnable()) {
            SnowflakeService snowflakeService = new SnowflakeService(properties.getSnowflake().getAddress(), properties.getSnowflake().getPort());
            return snowflakeService;
        }
        logger.warn("init leaf snowflake ignore properties is {}", properties);
        return null;
    }
}
