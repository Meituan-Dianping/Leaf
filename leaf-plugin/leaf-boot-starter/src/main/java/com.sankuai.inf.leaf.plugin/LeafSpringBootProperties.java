package com.sankuai.inf.leaf.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

/**
 * @author zhaodong.xzd (github.com/yaccc)
 * @date 2019/10/09
 */
@ConfigurationProperties("leaf")
@PropertySource("classpath:leaf.properties")
public class LeafSpringBootProperties {

    private boolean enable=false;

}
