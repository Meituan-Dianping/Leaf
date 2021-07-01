package com.sankuai.inf.leaf.segment.dao;

import org.apache.ibatis.annotations.Select;

import java.util.List;

/***
 *
 *
 * @author <a href="mailto:1934849492@qq.com">Darian</a> 
 * @date 2021/7/1  23:07
 */
public interface DailyIDAllocMapper {

    @Select("SELECT biz_tag FROM daily_leaf_alloc")
    List<String> getAllTags();
}
