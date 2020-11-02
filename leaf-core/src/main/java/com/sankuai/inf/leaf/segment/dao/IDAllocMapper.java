package com.sankuai.inf.leaf.segment.dao;

import com.sankuai.inf.leaf.segment.model.LeafAlloc;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * leaf_alloc Mapper
 * @author jiangyx3915
 */
public interface IDAllocMapper {
    /**
     * 返回所有leaf_alloc列表
     * @return          LeafAlloc列表
     */
    @Select("SELECT biz_tag, max_id, step, update_time FROM leaf_alloc")
    @Results(value = {
            @Result(column = "biz_tag", property = "key"),
            @Result(column = "max_id", property = "maxId"),
            @Result(column = "step", property = "step"),
            @Result(column = "update_time", property = "updateTime")
    })
    List<LeafAlloc> getAllLeafAllocs();

    /**
     * 获取指定业务标识的 max_id 和 步长
     * @param tag
     * @return
     */
    @Select("SELECT biz_tag, max_id, step FROM leaf_alloc WHERE biz_tag = #{tag}")
    @Results(value = {
            @Result(column = "biz_tag", property = "key"),
            @Result(column = "max_id", property = "maxId"),
            @Result(column = "step", property = "step")
    })
    LeafAlloc getLeafAlloc(@Param("tag") String tag);

    /**
     * 更新指定业务标识的 max_id
     * @param tag     业务标识
     */
    @Update("UPDATE leaf_alloc SET max_id = max_id + step WHERE biz_tag = #{tag}")
    void updateMaxId(@Param("tag") String tag);

    /**
     * 指定步长更新指定业务标识的max_id
     * @param leafAlloc    leafAlloc对象
     */
    @Update("UPDATE leaf_alloc SET max_id = max_id + #{step} WHERE biz_tag = #{key}")
    void updateMaxIdByCustomStep(@Param("leafAlloc") LeafAlloc leafAlloc);

    /**
     * 返回所有业务标识
     * @return      业务类别标识
     */
    @Select("SELECT biz_tag FROM leaf_alloc")
    List<String> getAllTags();
}
