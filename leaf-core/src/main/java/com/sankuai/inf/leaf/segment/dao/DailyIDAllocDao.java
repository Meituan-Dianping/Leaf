package com.sankuai.inf.leaf.segment.dao;

import com.sankuai.inf.leaf.segment.model.DailyLeafAlloc;

import java.util.List;

/***
 *
 *
 * @author <a href="mailto:1934849492@qq.com">Darian</a> 
 * @date 2021/7/1  21:35
 */
public interface DailyIDAllocDao {

    List<DailyLeafAlloc> getAllDailyLeafAlloc();

    List<String> getAllTags();
}
