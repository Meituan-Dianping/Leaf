package com.sankuai.inf.leaf.plugin;

import com.sankuai.inf.leaf.server.exception.InitException;
import com.sankuai.inf.leaf.server.service.SegmentService;
import com.sankuai.inf.leaf.server.service.SnowflakeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    @ConditionalOnProperty(prefix = "leaf.segment",value = "enable",matchIfMissing = false)
    public SegmentService initLeafSegmentStarter(){
        return null;
    }

    @Bean
    @ConditionalOnProperty(prefix = "leaf.snowflake",value = "enable",matchIfMissing = false)
    public SnowflakeService initLeafSnowflakeStarter() throws InitException {
        SnowflakeService snowflakeService =new SnowflakeService();
        return snowflakeService;
    }
}
