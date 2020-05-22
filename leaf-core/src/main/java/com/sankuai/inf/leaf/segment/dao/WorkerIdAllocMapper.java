package com.sankuai.inf.leaf.segment.dao;

import com.sankuai.inf.leaf.segment.model.LeafWorkerIdAlloc;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface WorkerIdAllocMapper {

    @Insert({
            "insert into leaf_workerid_alloc (ip_port," +
                    "ip,port,max_timestamp,create_time,update_time) ",
            "values (#{ipPort,jdbcType=VARCHAR}, #{ip,jdbcType=VARCHAR}, ",
            "#{port,jdbcType=VARCHAR}, #{maxTimestamp,jdbcType=TIMESTAMP}, ",
            "#{createTime,jdbcType=TIMESTAMP}, #{updateTime,jdbcType=TIMESTAMP})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertIfNotExist(@Param("leafWorkId") LeafWorkerIdAlloc leafWorkerIdAlloc);

    @Select("SELECT * FROM leaf_workerid_alloc WHERE ip_port = #{ipPort,jdbcType=VARCHAR}")
    @Results(value = {
            @Result(column = "id", property = "id"),
            @Result(column = "ip", property = "ip"),
            @Result(column = "port", property = "port"),
            @Result(column = "ip_port", property = "ipPort"),
            @Result(column = "max_timestamp", property = "maxTimestamp"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime")
    })
    List<LeafWorkerIdAlloc> getLeafWorkerIdAlloc(@Param("ipPort") String ipPort);

    @Update("UPDATE leaf_workerid_alloc SET max_timestamp = #{maxTimestamp,jdbcType=BIGINT} WHERE id = #{id,jdbcType=INTEGER} ")
    void updateMaxTimestamp(@Param("leafWorkId") LeafWorkerIdAlloc leafWorkerIdAlloc);

}
