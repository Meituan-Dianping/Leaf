package com.sankuai.inf.leaf.server;

/**
 *  ZK_NORMAL 注册中心为Zookeeper，,针对每个ip:port的workid是固定的
 *  就是根据ip+port在zookeeper创建一个永久的workId，一直使用
 *
 *  ZK_RECYCLE  注册中心为Zookeeper，workid可循环模式
 *  就是Leaf服务每次启动都去zookeeper中/notuse/路径下去取一个workId，
 *  移动到/inuse/路径下使用，使用完毕后会被放回zookeeper中/notuse/路径下，以供循环使用
 *
 *  MYSQL 注册中心为MySQL,针对每个ip:port的workid是固定的
 *
 *  LOCAL 注册中心为本地配置，针对每个会部署Leaf服务的ip和port，
 *  在项目的leaf.properties文件中将ip:port写入leaf.snowflake.local.workIdMap，启动时读取
 *
 */
public class SnowflakeMode {

    public static final String ZK_NORMAL = "zk_normal";

    public static final String ZK_RECYCLE = "zk_recycle";

    public static final String MYSQL = "mysql";

    public static final String LOCAL = "local";

}
