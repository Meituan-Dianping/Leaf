package com.sankuai.inf.leaf.segment;

import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Status;
import com.sankuai.inf.leaf.segment.dao.IDAllocDao;
import com.sankuai.inf.leaf.segment.model.*;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author mickle
 */
public class SegmentIDGenImpl implements IDGen {
    private static final Logger logger = LoggerFactory.getLogger(SegmentIDGenImpl.class);

    /**
     * IDCache未初始化成功时的异常码
     */
    private static final long EXCEPTION_ID_IDCACHE_INIT_FALSE = -1;
    /**
     * key不存在时的异常码
     */
    private static final long EXCEPTION_ID_KEY_NOT_EXISTS = -2;
    /**
     * SegmentBuffer中的两个Segment均未从DB中装载时的异常码
     */
    private static final long EXCEPTION_ID_TWO_SEGMENTS_ARE_NULL = -3;
    /**
     * 最大步长不超过100,0000
     */
    private static final int MAX_STEP = 1000000;
    /**
     * 一个Segment维持时间为15分钟
     */
    private static final long SEGMENT_DURATION = 15 * 60 * 1000L;
    private final ExecutorService service = new ThreadPoolExecutor(5, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new UpdateThreadFactory());
    /**
     * 初始化是否成功
     */
    private volatile boolean initSuccess = false;
    /**
     * 业务标识，双 Segment
     */
    private final Map<String, SegmentBuffer> cache = new ConcurrentHashMap<String, SegmentBuffer>();
    private IDAllocDao dao;
    /**
     * 增长因子
     */
    private final int factory = 2;

    public static class UpdateThreadFactory implements ThreadFactory {

        private static int threadInitNumber = 0;

        private static synchronized int nextThreadNum() {
            return threadInitNumber++;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "Thread-Segment-Update-" + nextThreadNum());
        }
    }

    @Override
    public boolean init() {
        logger.info("Init ...");
        // 确保加载到kv后才初始化成功
        updateCacheFromDb();
        initSuccess = true;
        updateCacheFromDbAtEveryMinute();
        return initSuccess;
    }

    /**
     * 定时每分钟从数据库中加载tag到cache中
     *
     * 对比数据库中和cache中的业务，新增则往cache中添加，删除则从cache中删除
     */
    private void updateCacheFromDbAtEveryMinute() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setName("check-idCache-thread");
                t.setDaemon(true);
                return t;
            }
        });
        service.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                updateCacheFromDb();
            }
        }, 60, 60, TimeUnit.SECONDS);
    }

    /**
     * 加载数据库中的业务tag到cache
     *
     * 添加增量更新的tag和移除失效的tag
     */
    private void updateCacheFromDb() {
        logger.info("update cache from db");
        StopWatch sw = new Slf4JStopWatch();
        try {
            // 获取数据库中所有业务标识
            List<String> dbTags = dao.getAllTags();

            // 没有业务标识直接返回
            if (dbTags == null || dbTags.isEmpty()) {
                return;
            }
            // cache 中已存在的业务 tag 列表
            List<String> cacheTags = new ArrayList<String>(cache.keySet());
            Set<String> insertTagsSet = new HashSet<>(dbTags);
            Set<String> removeTagsSet = new HashSet<>(cacheTags);
//            for (String tmp : cacheTags) {
//              insertTagsSet.remove(tmp);
//            }
            // 感觉可以优化： 数据库中 tag 和 cache 中对比，找出数据库新增的 tag
            insertTagsSet.removeAll(cacheTags);
            // 将数据库中新增的tag加入cache
            for (String tag : insertTagsSet) {
                SegmentBuffer buffer = new SegmentBuffer();
                buffer.setKey(tag);
                Segment segment = buffer.getCurrent();
                segment.setValue(new AtomicLong(0));
                segment.setMax(0);
                segment.setStep(0);
                cache.put(tag, buffer);
                logger.info("Add tag {} from db to IdCache, SegmentBuffer {}", tag, buffer);
            }
            // cache 中已失效的 tag 从 cache 删除
//            for (String tmp : dbTags) {
//              removeTagsSet.remove(tmp);
//            }
            // 数据库与cache对比，找出数据库库中删除的tag
            removeTagsSet.removeAll(dbTags);
            // 将数据库中删除的tag从cache中删除
            for (String tag : removeTagsSet) {
                cache.remove(tag);
                logger.info("Remove tag {} from IdCache", tag);
            }
        } catch (Exception e) {
            logger.warn("update cache from db exception", e);
        } finally {
            sw.stop("updateCacheFromDb");
        }
    }

    /**
     * 获取指定业务tag下一id
     * @param key         业务tag
     * @return            result
     */
    @Override
    public Result get(final String key) {
        // 如果生成器初始化失败，则返回未成功初始化异常码
        if (!initSuccess) {
            return new Result(EXCEPTION_ID_IDCACHE_INIT_FALSE, Status.EXCEPTION);
        }
        // cache中有业务tag
        if (cache.containsKey(key)) {
            // 获取业务对应的SegmentBuffer
            SegmentBuffer buffer = cache.get(key);
            // 双重判断SegmentBuffer是否初始化完毕
            if (!buffer.isInitOk()) {
                synchronized (buffer) {
                    if (!buffer.isInitOk()) {
                        try {
                            // 如果未初始化成功则初始化当前SegmentBuffer选择的Segment
                            updateSegmentFromDb(key, buffer.getCurrent());
                            logger.info("Init buffer. Update leafkey {} {} from db", key, buffer.getCurrent());
                            // 设置SegmentBuffer初始化成功
                            buffer.setInitOk(true);
                        } catch (Exception e) {
                            logger.warn("Init buffer {} exception", buffer.getCurrent(), e);
                        }
                    }
                }
            }
            // 获取该业务tag的下个id
            return getIdFromSegmentBuffer(cache.get(key));
        }
        // cache中无业务tag对象的SegmentBuffer缓存则返回业务不存在异常码
        return new Result(EXCEPTION_ID_KEY_NOT_EXISTS, Status.EXCEPTION);
    }

    /**
     * 从数据库加载数据到Segment中
     * @param key               业务tag
     * @param segment           对应SegmentBuffer中当前选中的Segment
     */
    public void updateSegmentFromDb(String key, Segment segment) {
        StopWatch sw = new Slf4JStopWatch();
        SegmentBuffer buffer = segment.getBuffer();
        LeafAlloc leafAlloc;
        // buffer 为初始化成功
        if (!buffer.isInitOk()) {
            // 更新 max_id 为 max_id + step
            leafAlloc = dao.updateMaxIdAndGetLeafAlloc(key);
            // 设置buffer中的step为DB中的step
            buffer.setStep(leafAlloc.getStep());
            // 设置buffer中的min_step为DB中的step
            buffer.setMinStep(leafAlloc.getStep());
        } else if (buffer.getUpdateTimestamp() == 0) {
            // buffer 初始化成功，但是未设置过修改时间
            leafAlloc = dao.updateMaxIdAndGetLeafAlloc(key);
            // 设置修改时间为当前时间
            buffer.setUpdateTimestamp(System.currentTimeMillis());
            // 设置buffer中的step为DB中的step
            buffer.setStep(leafAlloc.getStep());
            // 设置buffer中的min_step为DB中的step
            buffer.setMinStep(leafAlloc.getStep());
        } else {
            // buffer成功初始化并且已设置过修改时间

            // 当前时间和上一次修改时间距离的毫秒数
            long duration = System.currentTimeMillis() - buffer.getUpdateTimestamp();
            // 下一次增加数量
            int nextStep = buffer.getStep();
            // 相差毫秒数小于一个Segment维持时间
            // 如果距离上一次更新时间小于一个Segment维持时间
            // 则表示获取id的速率较快，则增大step减少扩容时间
            // 在step不大于MAX_STEP时，则进行扩容
            if (duration < SEGMENT_DURATION) {
//                if (nextStep * 2 > MAX_STEP) {
//                    //do nothing
//                } else {
//                    nextStep = nextStep * 2;
//                }
                if (nextStep * factory <= MAX_STEP) {
                    nextStep = nextStep * factory;
                }
            } else if (duration >= SEGMENT_DURATION * factory) {
                // 如果距离上一次更新时间大于2倍一个Segment维持时间
                // 表示获取id速度较慢，则进行缩容
                nextStep = nextStep / 2 >= buffer.getMinStep() ? nextStep / factory : nextStep;
            }
            logger.info("leafKey[{}], step[{}], duration[{}mins], nextStep[{}]", key, buffer.getStep(), String.format("%.2f",((double)duration / (1000 * 60))), nextStep);
            LeafAlloc temp = new LeafAlloc();
            temp.setKey(key);
            temp.setStep(nextStep);
            // 更新改业务tag的max_id
            leafAlloc = dao.updateMaxIdByCustomStepAndGetLeafAlloc(temp);
            buffer.setUpdateTimestamp(System.currentTimeMillis());
            // 设置下一次增长数量
            buffer.setStep(nextStep);
            // 设置下一次最小增长数量
            buffer.setMinStep(leafAlloc.getStep());
        }
        // 起始值
        long value = leafAlloc.getMaxId() - buffer.getStep();
        // 设置当前segment的当前值
        segment.getValue().set(value);
        // 设置当前segment的max_id（结束值）
        segment.setMax(leafAlloc.getMaxId());
        // 设置当前segment的step
        segment.setStep(buffer.getStep());
        sw.stop("updateSegmentFromDb", key + " " + segment);
    }

    /**
     * 从SegmentBuffer中获取下个业务id
     * @param buffer            SegmentBuffer
     * @return                  id
     */
    public Result getIdFromSegmentBuffer(final SegmentBuffer buffer) {
        while (true) {
            // 获取读锁
            buffer.rLock().lock();
            try {
                // 获取当前正在使用的segment
                final Segment segment = buffer.getCurrent();
                // 下一个segment为不可切换状态 && 当前segment已使用超过10% && buffer的线程状态为false
                // 预热下一个segment
                if (!buffer.isNextReady() && (segment.getIdle() < 0.9 * segment.getStep())
                    && buffer.getThreadRunning().compareAndSet(false, true)) {
                    service.execute(new Runnable() {
                        @Override
                        public void run() {
                        // 下一个待切换segment
                        Segment next = buffer.getSegments()[buffer.nextPos()];
                        boolean updateOk = false;
                        try {
                            // 提前将数据加载到待切换segment
                            updateSegmentFromDb(buffer.getKey(), next);
                            updateOk = true;
                            logger.info("update segment {} from db {}", buffer.getKey(), next);
                        } catch (Exception e) {
                            logger.warn(buffer.getKey() + " updateSegmentFromDb exception", e);
                        } finally {
                            // 如果数据加载成功
                            if (updateOk) {
                                // 获得写锁
                                buffer.wLock().lock();
                                // 设置下一个segment可切换状态为true
                                buffer.setNextReady(true);
                                // 设置buffer线程运行状态为false
                                buffer.getThreadRunning().set(false);
                                // 释放写锁
                                buffer.wLock().unlock();
                            } else {
                                buffer.getThreadRunning().set(false);
                            }
                        }
                        }
                    });
                }
                // 获取下一个id
                long value = segment.getValue().getAndIncrement();
                // 如果未超过最大则直接返回
                if (value < segment.getMax()) {
                    return new Result(value, Status.SUCCESS);
                }
            } finally {
                // 释放读锁
                buffer.rLock().unlock();
            }

            // 上述预热下一个segment出现异常，并且buffer线程运行状态还未设置成false时
            // 进行等待 100 秒，超时则抛出异常
            waitAndSleep(buffer);

            // 获得写锁
            buffer.wLock().lock();
            try {
                final Segment segment = buffer.getCurrent();
                // 获取下一个id
                long value = segment.getValue().getAndIncrement();
                // 如果未超过最大则直接返回
                if (value < segment.getMax()) {
                    return new Result(value, Status.SUCCESS);
                }
                // 如果号段已耗尽则准备切换下一个准备好的segment
                if (buffer.isNextReady()) {
                    buffer.switchPos();
                    buffer.setNextReady(false);
                } else {
                    // 下个 segment 未成功初始化则返回未从DB中装载的异常码
                    logger.error("Both two segments in {} are not ready!", buffer);
                    return new Result(EXCEPTION_ID_TWO_SEGMENTS_ARE_NULL, Status.EXCEPTION);
                }
            } finally {
                // 释放写锁
                buffer.wLock().unlock();
            }
        }
    }

    /**
     * 睡眠并等待buffer中线程运行状态为false
     * @param buffer
     */
    private void waitAndSleep(SegmentBuffer buffer) {
        int roll = 0;
        while (buffer.getThreadRunning().get()) {
            roll += 1;
            if(roll > 10000) {
                try {
                    TimeUnit.MILLISECONDS.sleep(10);
                    break;
                } catch (InterruptedException e) {
                    logger.warn("Thread {} Interrupted",Thread.currentThread().getName());
                    break;
                }
            }
        }
    }

    public List<LeafAlloc> getAllLeafAllocs() {
        return dao.getAllLeafAllocs();
    }

    public Map<String, SegmentBuffer> getCache() {
        return cache;
    }

    public IDAllocDao getDao() {
        return dao;
    }

    public void setDao(IDAllocDao dao) {
        this.dao = dao;
    }
}
