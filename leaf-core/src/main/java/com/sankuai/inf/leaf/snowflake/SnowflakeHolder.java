package com.sankuai.inf.leaf.snowflake;

/**
 * @author yangjunhui
 * @date 2020/4/29 2:48 下午
 */
public interface SnowflakeHolder {
    //holder初始化方法，执行init()成功，返回true之后才能调用getWorkerID获取workID
    boolean init();

    int getWorkerID();

}
