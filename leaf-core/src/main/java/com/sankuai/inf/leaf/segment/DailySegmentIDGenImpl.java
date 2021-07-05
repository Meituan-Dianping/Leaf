package com.sankuai.inf.leaf.segment;

import com.sankuai.inf.leaf.common.DateUtils;
import com.sankuai.inf.leaf.common.ListUtils;
import com.sankuai.inf.leaf.segment.dao.DailyIDAllocDao;
import org.perf4j.StopWatch;
import org.perf4j.slf4j.Slf4JStopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/***
 *
 *
 * @author <a href="mailto:1934849492@qq.com">Darian</a> 
 * @date 2021/7/1  21:32
 */
public class DailySegmentIDGenImpl extends SegmentIDGenImpl {
    private static final Logger logger = LoggerFactory.getLogger(DailySegmentIDGenImpl.class);

    /**
     * 十天以前
     */
    private volatile static List<String> BEFORE_TEN_DAYS = new ArrayList<>();

    /**
     * 十天以后
     */
    private volatile static List<String> AFTER_TEN_DAYS = new ArrayList<>();

    /**
     * 刷新的当前天
     */
    private volatile String NOW_DAY;

//    /**
//     * 刷新的次数
//     */
//    private volatile int initCount = 0;

    private DailyIDAllocDao dailyIDAllocDao;

    public void setDailyIDAllocDao(DailyIDAllocDao dailyIDAllocDao) {
        this.dailyIDAllocDao = dailyIDAllocDao;
    }

    @Override
    public boolean init() {
        logger.info("DailySegmentIdGenImpl Init ...");
        // 确保加载到kv后才初始化成功
        updateCacheFromDb();
        initOK = true;
        updateCacheFromDbAtEveryMinute();
        return initOK;
    }

    @Override
    protected void updateCacheFromDb() {
        logger.info("DailySegmentIdGenImpl updateCacheFromDb ... ");
        insertLeafAllocByDailyLeafAlloc();
        super.updateCacheFromDb();
    }

    /**
     * 从 daily_leaf_alloc 每日表，更新到 leaf_alloc 表，随机 5-10 天，使得可以打散开来，避免集中插入
     */
    private void insertLeafAllocByDailyLeafAlloc() {
        logger.info("DailySegmentIdGenImpl insertLeafAllocByDailyLeafAlloc ...");
        StopWatch sw = new Slf4JStopWatch();
        try {
            // 查询 daily_leaf_alloc 表
            List<String> dailyAllocAllTags = dailyIDAllocDao.getAllTags();
            if (dailyAllocAllTags == null || dailyAllocAllTags.isEmpty()) {
                return;
            }

            String nowDay = DateUtils.formatyyyyMMdd(new Date());
            if (NOW_DAY == null || NOW_DAY.length() == 0 || !NOW_DAY.equals(nowDay)) {
                // 说明第一次启动或者不是同一天
                initBeforeTenDaysAndAfterTenDays();
//                initCount = 0;
            }

//            initCount++;

            List<String> allAllocTagList = dao.getAllTags();

            if (allAllocTagList != null && allAllocTagList.size() > 0) {
                List<String> deleteAllocTagList = new ArrayList<>();

                for (String dailyAllocAllTag : dailyAllocAllTags) {
                    for (String beforeTenDay : BEFORE_TEN_DAYS) {
                        String deleteKey = dailyAllocAllTag + beforeTenDay;
                        if (allAllocTagList.contains(deleteKey)) {
                            deleteAllocTagList.add(deleteKey);
                        }
                    }
                }


                if (deleteAllocTagList == null || deleteAllocTagList.isEmpty()) {
                    logger.warn("deleteAllocTagList null || empty");
                } else {
                    List<List<String>> deleteAllocTagListList = ListUtils.splitList(deleteAllocTagList, 200);
                    for (List<String> deleteAllocTags : deleteAllocTagListList) {
                        logger.info("deleteAllocTags list : " + deleteAllocTags);
                        dao.deleteTags(deleteAllocTags);
                    }
                }
            }


//            if (initCount < 4) {
            // 随机 5-10 天
            Set<String> insertTOLeafAllocTagList = new HashSet<>();
            for (String allTag : dailyAllocAllTags) {
                int dailyCount = ThreadLocalRandom.current().nextInt(3, 5);
                for (int i = 0; i < dailyCount; i++) {
                    insertTOLeafAllocTagList.add(allTag + (AFTER_TEN_DAYS.get(i)));
                }
            }

            if (insertTOLeafAllocTagList == null || insertTOLeafAllocTagList.isEmpty()) {
                return;
            }

            for (String allocTag : allAllocTagList) {
                insertTOLeafAllocTagList.remove(allocTag);
            }

            try {
                if (insertTOLeafAllocTagList == null || insertTOLeafAllocTagList.isEmpty()) {
                    logger.info("insertTOLeafAllocTagList tagList empty, not need");
                    return;
                }

                // 生成每日的序列号生成器
                dao.batchInsert(new ArrayList<>(insertTOLeafAllocTagList));
            } catch (Exception e) {
                logger.warn("insertTOLeafAllocTagList db exception", e);
            }

//            }

        } catch (Exception e) {
            logger.warn("insertLeafAllocByDailyLeafAlloc cache from db exception", e);
        } finally {
            sw.stop("insertLeafAllocByDailyLeafAlloc");
        }
    }

    /**
     * 初始10天前再往前十天，初始化，今天再往后十天
     */
    private void initBeforeTenDaysAndAfterTenDays() {
        AFTER_TEN_DAYS = new ArrayList<>();
        for (int i = 1; i < 11; i++) {
            AFTER_TEN_DAYS.add(DateUtils.formatyyyyMMdd(DateUtils.addDay(new Date(), i)));
        }
        logger.info("AFTER_TEN_DAYS:" + AFTER_TEN_DAYS);

        BEFORE_TEN_DAYS = new ArrayList<>();
        for (int i = -20; i < -10; i++) {
            BEFORE_TEN_DAYS.add(DateUtils.formatyyyyMMdd(DateUtils.addDay(new Date(), i)));
        }
        logger.info("BEFORE_TEN_DAYS:" + BEFORE_TEN_DAYS);
    }
}
