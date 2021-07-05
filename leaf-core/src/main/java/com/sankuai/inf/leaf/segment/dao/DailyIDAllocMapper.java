package com.sankuai.inf.leaf.segment.dao;

import com.sankuai.inf.leaf.segment.model.DailyLeafAlloc;
import com.sankuai.inf.leaf.segment.model.LeafAlloc;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/***
 *
 *
 * @author <a href="mailto:1934849492@qq.com">Darian</a> 
 * @date 2021/7/1  23:07
 */
public interface DailyIDAllocMapper {

    @Select("SELECT biz_tag, max_id, step, description, update_time FROM leaf_alloc")
    @Results(value = {
            @Result(column = "biz_tag", property = "key"),
            @Result(column = "max_id", property = "maxId"),
            @Result(column = "step", property = "step"),
            @Result(column = "description", property = "description"),
            @Result(column = "update_time", property = "updateTime")
    })
    List<DailyLeafAlloc> getAllDailyLeafAllocs();


    @Select("SELECT biz_tag FROM daily_leaf_alloc")
    List<String> getAllTags();
}
