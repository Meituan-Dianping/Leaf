package com.sankuai.inf.leaf.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

/**
 * 配置文件工厂
 *
 * 加载 leaf.properties 配置文件
 * @author mickle
 */
public class PropertyFactory {
    private static final Logger logger = LoggerFactory.getLogger(PropertyFactory.class);

    /**
     * leaf.properties 中的配置
     */
    private static final Properties PROPERTIES = new Properties();

    // 读取配置文件并加载
    static {
        try {
          PROPERTIES.load(PropertyFactory.class.getClassLoader().getResourceAsStream("leaf.properties"));
        } catch (IOException e) {
            logger.warn("Load Properties Ex", e);
        }
    }
    public static Properties getProperties() {
        return PROPERTIES;
    }
}
