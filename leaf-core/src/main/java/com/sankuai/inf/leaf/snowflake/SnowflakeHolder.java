package com.sankuai.inf.leaf.snowflake;

/**
 * @author yangjunhui
 * @date 2020/4/29 2:48 下午
 */
public interface SnowflakeHolder {
    //holder初始化方法，执行init()成功，返回true之后才能调用getWorkerID获取workID
    boolean init();

    int getWorkerID();

    //当使用的WorkIdMode是RRECYCLABLE时，并且与zookeeper失去连接时间过长时，应该停止生成id并抛出异常
    public boolean getShouldGenerateContinue();

}
