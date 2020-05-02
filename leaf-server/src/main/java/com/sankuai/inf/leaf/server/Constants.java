package com.sankuai.inf.leaf.server;

public class Constants {
    public static final String LEAF_SEGMENT_ENABLE = "leaf.segment.enable";
    public static final String LEAF_JDBC_URL = "leaf.jdbc.url";
    public static final String LEAF_JDBC_USERNAME = "leaf.jdbc.username";
    public static final String LEAF_JDBC_PASSWORD = "leaf.jdbc.password";

    public static final String LEAF_SNOWFLAKE_ENABLE = "leaf.snowflake.enable";

    //注册中心的模式 zk，mysql或local
    public static final String LEAF_SNOWFLAKE_MODE = "leaf.snowflake.mode";

    //注册中心为zk模式时的相关配置
    public static final String LEAF_SNOWFLAKE_PORT = "leaf.snowflake.port";
    public static final String LEAF_SNOWFLAKE_ZK_ADDRESS = "leaf.snowflake.zk.address";

    //注册中心为local模式时的相关配置
    public static final String LEAF_SNOWFLAKE_LOCAL_WORKIDMAP = "leaf.snowflake.local.workIdMap";

}
