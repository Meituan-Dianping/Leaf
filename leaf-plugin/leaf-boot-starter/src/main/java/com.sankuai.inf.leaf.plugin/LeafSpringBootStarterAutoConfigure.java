package com.sankuai.inf.leaf.plugin;

import com.sankuai.inf.leaf.exception.InitException;
import com.sankuai.inf.leaf.service.SegmentService;
import com.sankuai.inf.leaf.service.SnowflakeService;
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
    @Autowired
    private LeafSpringBootProperties properties;

    @Bean
    public SegmentService initLeafSegmentStarter() throws Exception {
        if (properties != null && properties.getSegment() != null && properties.getSegment().isEnable()) {
            SegmentService segmentService = new SegmentService();
            return segmentService;
        }
        return null;
    }

    @Bean
    public SnowflakeService initLeafSnowflakeStarter() throws InitException {
        if (properties != null && properties.getSegment() != null && properties.getSegment().isEnable()) {
            SnowflakeService snowflakeService = new SnowflakeService();
            return snowflakeService;
        }
        return null;
    }
}
