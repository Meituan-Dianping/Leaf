package com.sankuai.inf.leaf.segment;

import com.alibaba.druid.pool.DruidDataSource;
import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.PropertyFactory;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Status;
import com.sankuai.inf.leaf.segment.dao.IDAllocDao;
import com.sankuai.inf.leaf.segment.dao.impl.IDAllocDaoImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;


public class IDGenServiceTest {
    public static final AtomicInteger threadSeq = new AtomicInteger(0);
    IDGen idGen;
    DruidDataSource dataSource;
    @Before
    public void before() throws IOException, SQLException {
        // Load Db Config
        Properties properties = PropertyFactory.getProperties();

        // Config dataSource
        dataSource = new DruidDataSource();
        dataSource.setUrl(properties.getProperty("jdbc.url"));
        dataSource.setUsername(properties.getProperty("jdbc.username"));
        dataSource.setPassword(properties.getProperty("jdbc.password"));
        dataSource.init();

        // Config Dao
        IDAllocDao dao = new IDAllocDaoImpl(dataSource);

        // Config ID Gen
        idGen = new SegmentIDGenImpl();
        ((SegmentIDGenImpl) idGen).setDao(dao);
        idGen.init();
    }
    @Test
    public void testGetId() {
        for (int i = 0; i < 100; ++i) {
            Result r = idGen.get("leaf-segment-test");
            System.out.println(r);
        }
    }
    @After
    public void after() {
       dataSource.close();
    }


    @Test
    public void testConcurrentAcquire() throws Exception{
        int threadNum=200;
        //每个线程取号1000次
        int takeNumberTimes = 1000;
        /**
         * 修改数据库中的步长为10
         */
        int step=10;
        ConcurrentHashMap<String, Long> concurrentHashMap = new ConcurrentHashMap();
        CountDownLatch waitForRunLatch = new CountDownLatch(1);
        CountDownLatch mainThreadLatch = new CountDownLatch(threadNum);
        String tag = "leaf-segment-test";
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadNum; i++) {
            new Thread(new TakeNumberTask(mainThreadLatch, waitForRunLatch, idGen, takeNumberTimes, tag, concurrentHashMap,failCount)).start();
        }

        StopWatch mainThreadWatch = new Slf4JStopWatch();
        waitForRunLatch.countDown();
        mainThreadLatch.await();
        final long elapsedTime = mainThreadWatch.getElapsedTime();
        mainThreadWatch.stop("所有线程取号完毕");
        System.out.println("所有线程取号完毕：总共耗时：" + elapsedTime + " 总共获取失败次数：" + failCount.get() );
        final Map<String, Long> sortedMap = sortMapByValue(concurrentHashMap);
        final Iterator<Map.Entry<String, Long>> iterator = sortedMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, Long> next = iterator.next();
            System.out.println("线程名称："+ next.getKey()+" 耗时："+next.getValue());
        }

    }

    public static Map<String, Long> sortMapByValue(Map<String, Long> oriMap) {
        if (oriMap == null || oriMap.isEmpty()) {
            return null;
        }
        Map<String, Long> sortedMap = new LinkedHashMap<String, Long>();
        List<Map.Entry<String, Long>> entryList = new ArrayList<Map.Entry<String, Long>>(
                oriMap.entrySet());
        Collections.sort(entryList, new MapValueComparator());

        Iterator<Map.Entry<String, Long>> iter = entryList.iterator();
        Map.Entry<String, Long> tmpEntry = null;
        while (iter.hasNext()) {
            tmpEntry = iter.next();
            sortedMap.put(tmpEntry.getKey(), tmpEntry.getValue());
        }
        return sortedMap;
    }



    static class TakeNumberTask implements Runnable {
        private CountDownLatch waitForRunLatch;
        private CountDownLatch mainThreadLatch;
        private IDGen idGen;
        private int takeNumberTimes;
        private String tag;
        private ConcurrentHashMap concurrentHashMap;
        private AtomicInteger failCount ;

        public TakeNumberTask(CountDownLatch mainThreadLatch,CountDownLatch waitForRunLatch, IDGen idGen,int takeNumerTimes,String tag,ConcurrentHashMap concurrentHashMap,AtomicInteger failCount) {
            this.mainThreadLatch = mainThreadLatch;
            this.waitForRunLatch = waitForRunLatch;
            this.idGen = idGen;
            this.takeNumberTimes = takeNumerTimes;
            this.tag = tag;
            this.concurrentHashMap = concurrentHashMap;
            this.failCount = failCount;
        }

        @Override
        public void run() {
            Thread thread = Thread.currentThread();
            String threadName = "UserThread-takeNumber-seq-" + threadSeq.incrementAndGet();
            thread.setName(threadName);
            try {
                waitForRunLatch.await();

                StopWatch stopWatch = new Slf4JStopWatch();
                int takeNumFail = 0;
                for (int i = 0; i < takeNumberTimes; i++) {
                    final Result result = idGen.get(tag);
                    if (result.getStatus().equals(Status.EXCEPTION)) {
                        takeNumFail++;
                    }
                }
                final long elapsedTime = stopWatch.getElapsedTime();
                stopWatch.stop("take number complete");
                concurrentHashMap.put(threadName, elapsedTime);
                failCount.addAndGet(takeNumFail);
                mainThreadLatch.countDown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    static class MapValueComparator implements Comparator<Map.Entry<String, Long>> {

        @Override
        public int compare(Map.Entry<String, Long> me1, Map.Entry<String, Long> me2) {

            return me2.getValue().compareTo(me1.getValue());
        }
    }

}
