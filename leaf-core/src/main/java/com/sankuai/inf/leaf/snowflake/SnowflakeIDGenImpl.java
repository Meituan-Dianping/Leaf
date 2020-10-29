package com.sankuai.inf.leaf.snowflake;

import com.google.common.base.Preconditions;
import com.sankuai.inf.leaf.IDGen;
import com.sankuai.inf.leaf.common.Result;
import com.sankuai.inf.leaf.common.Status;
import com.sankuai.inf.leaf.common.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * 发号器（雪花算法实现）
 * @author mickle
 */
public class SnowflakeIDGenImpl implements IDGen {

    @Override
    public boolean init() {
        return true;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SnowflakeIDGenImpl.class);

    /**
     * 起始时间戳，用于用当前时间戳减去这个时间戳，算出偏移量
     * 默认为：1288834974657L  Thu Nov 04 2010 09:42:54 GMT+0800 (中国标准时间)
     */
    private final long twepoch;
    /**
     * 机器号占用位数
     */
    private final long workerIdBits = 10L;
    /**
     * 最大能够分配的 worker id
     *
     * ~(-1L << workerIdBits) = -1L ^ (-1L << workerIdBits) = 2^workerIdBits -1
     *
     * 2 ^ 10 - 1 = 1024 -1 = 1023
     */
    private final long maxWorkerId = ~(-1L << workerIdBits);
    /**
     * 12为序列号
     */
    private final long sequenceBits = 12L;
    /**
     * 机器号偏移位数 12
     */
    private final long workerIdShift = sequenceBits;
    /**
     * 时间戳偏移位数 10 + 12 = 22
     */
    private final long timestampLeftShift = sequenceBits + workerIdBits;
    /**
     * 序列号掩码，保证与序列号掩码进行 & 操作后得到的值始终处于 0 ~ 序列号掩码值
     * 默认掩码值为 2^12 - 1 = 4095
     */
    private final long sequenceMask = ~(-1L << sequenceBits);
    /**
     * 机器号
     */
    private long workerId;
    /**
     * 起始序列
     */
    private long sequence = 0L;
    /**
     * 上一次发号时间
     */
    private long lastTimestamp = -1L;
    private static final Random RANDOM = new Random();
    /**
     * 最小偏差时间值
     * 默认5毫秒
     */
    private final int minOffset = 5;

    public SnowflakeIDGenImpl(String zkAddress, int port) {
        //Thu Nov 04 2010 09:42:54 GMT+0800 (中国标准时间)
        this(zkAddress, port, 1288834974657L);
    }

    /**
     * @param zkAddress zk地址
     * @param port      snowflake监听端口
     * @param twepoch   起始的时间戳
     */
    public SnowflakeIDGenImpl(String zkAddress, int port, long twepoch) {
        this.twepoch = twepoch;
        Preconditions.checkArgument(timeGen() > twepoch, "Snowflake not support twepoch gt currentTime");
        final String ip = Utils.getIp();
        SnowflakeZookeeperHolder holder = new SnowflakeZookeeperHolder(ip, String.valueOf(port), zkAddress);
        LOGGER.info("twepoch:{} ,ip:{} ,zkAddress:{} port:{}", twepoch, ip, zkAddress, port);
        boolean initFlag = holder.init();
        if (initFlag) {
            workerId = holder.getWorkerId();
            LOGGER.info("START SUCCESS USE ZK WORKERID-{}", workerId);
        } else {
            Preconditions.checkArgument(initFlag, "Snowflake Id Gen is not init ok");
        }
        Preconditions.checkArgument(workerId >= 0 && workerId <= maxWorkerId, "workerID must gte 0 and lte 1023");
    }

    @Override
    public synchronized Result get(String key) {
        // 获取当前时间戳
        long timestamp = timeGen();
        // 如果当前时间小于上次发号时间
        if (timestamp < lastTimestamp) {
            // 计算偏差值
            long offset = lastTimestamp - timestamp;
            // 如果时钟偏差超过最小偏差值
            if (offset <= minOffset) {
                try {
                    // 等待2倍偏差值时间
                    wait(offset << 1);
                    // 重新获取当前时间与上次发号时间进行对比
                    // 如果仍小于最小偏差值则抛出异常
                    timestamp = timeGen();
                    if (timestamp < lastTimestamp) {
                        return new Result(-1, Status.EXCEPTION);
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("wait interrupted");
                    return new Result(-2, Status.EXCEPTION);
                }
            } else {
                // 时间偏差过大则直接抛出异常，手工进行时钟修正
                return new Result(-3, Status.EXCEPTION);
            }
        }
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            if (sequence == 0) {
                //seq 为0的时候表示是下一毫秒时间开始对seq做随机
                sequence = RANDOM.nextInt(100);
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            //如果是新的ms开始
            sequence = RANDOM.nextInt(100);
        }
        lastTimestamp = timestamp;
        long id = ((timestamp - twepoch) << timestampLeftShift) | (workerId << workerIdShift) | sequence;
        return new Result(id, Status.SUCCESS);

    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳(毫秒)
     * @return            当前时间戳(毫秒)
     */
    protected long timeGen() {
        return System.currentTimeMillis();
    }

    public long getWorkerId() {
        return workerId;
    }

}
