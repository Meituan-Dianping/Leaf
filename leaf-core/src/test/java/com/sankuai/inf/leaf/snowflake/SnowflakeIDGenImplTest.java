package com.sankuai.inf.leaf.snowflake;

import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.PropertyFactory;
import com.sankuai.inf.leaf.common.Result;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.*;

public class SnowflakeIDGenImplTest {
    @Test
    public void testGetId() throws Exception {
        Properties properties = PropertyFactory.getProperties();
        final IDGen idGen = new SnowflakeIDGenImpl(properties.getProperty("leaf.zk.list"), 2181,"leaf");
        int size = 1000;
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        final CountDownLatch countDownLatch = new CountDownLatch(size);
        final CyclicBarrier cyclicBarrier = new CyclicBarrier(size);
        final Map<Long,Long> resMap = new ConcurrentHashMap<>();
        for(int j = 0; j< size; j++){
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        cyclicBarrier.await();
                    } catch (Exception e) {

                    }
                    for (int i = 0; i < 1000; ++i) {
                        Result r = idGen.get("a");
                        resMap.put(r.getId(),System.currentTimeMillis());
                    }
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        executorService.shutdown();

        System.out.println("----------> size: " + resMap.keySet().size());
    }
}
