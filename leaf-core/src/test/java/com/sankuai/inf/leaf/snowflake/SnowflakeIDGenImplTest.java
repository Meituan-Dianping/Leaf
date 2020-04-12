package com.sankuai.inf.leaf.snowflake;

import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.PropertyFactory;
import com.sankuai.inf.leaf.common.Result;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.util.Properties;

public class SnowflakeIDGenImplTest {
    @Test
    public void testGetId() throws Exception {
        Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("leaf.properties"));

        IDGen idGen = new SnowflakeIDGenImpl(properties.getProperty("leaf.zk.list"), 8080);
        for (int i = 1; i < 1000; ++i) {
            Result r = idGen.get("a");
            System.out.println(r);
        }
    }
}
