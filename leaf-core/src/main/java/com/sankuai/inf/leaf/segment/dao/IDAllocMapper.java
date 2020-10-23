package com.sankuai.inf.leaf.segment.dao;

import com.sankuai.inf.leaf.segment.model.LeafAlloc;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * IDAlloc Mapper
 *
 * @author mickle
 */
public interface IDAllocMapper {

    /**
     * 获取 leaf_alloc 对象列表
     * @return                  leaf_alloc 对象列表
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
     * 根据业务信息获取指定 leaf_alloc 对象
     * @param tag                 业务标识
     * @return                    leaf_alloc 对象
     */
    @Select("SELECT biz_tag, max_id, step FROM leaf_alloc WHERE biz_tag = #{tag}")
    @Results(value = {
            @Result(column = "biz_tag", property = "key"),
            @Result(column = "max_id", property = "maxId"),
            @Result(column = "step", property = "step")
    })
    LeafAlloc getLeafAlloc(@Param("tag") String tag);

    /**
     * 更新 leaf_alloc 对象 max_id
     * @param tag           业务标识
     */
    @Update("UPDATE leaf_alloc SET max_id = max_id + step WHERE biz_tag = #{tag}")
    void updateMaxId(@Param("tag") String tag);

    /**
     * 设置动态 step 更新 max_id
     * @param leafAlloc     leaf_alloc 对象
     */
    @Update("UPDATE leaf_alloc SET max_id = max_id + #{step} WHERE biz_tag = #{key}")
    void updateMaxIdByCustomStep(@Param("leafAlloc") LeafAlloc leafAlloc);

    /**
     * 获取所有业务标识
     * @return            业务标识列表
     */
    @Select("SELECT biz_tag FROM leaf_alloc")
    List<String> getAllTags();
}
