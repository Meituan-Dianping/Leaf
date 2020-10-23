package com.sankuai.inf.leaf.segment.dao;

import com.sankuai.inf.leaf.segment.model.LeafAlloc;

import java.util.List;

/**
 * @author mickle
 */
public interface IDAllocDao {
    /**
     * 获取 leaf_alloc 对象列表
     * @return                  leaf_alloc 对象列表
     */
    List<LeafAlloc> getAllLeafAllocs();

    /**
     * 更新 max_id 并获取最新 leaf_alloc 对象
     * @param tag               业务标识
     * @return                  leaf_alloc 对象
     */
    LeafAlloc updateMaxIdAndGetLeafAlloc(String tag);

    /**
     * 设置动态 step 更新 max_id
     * @param leafAlloc     leaf_alloc 对象
     * @return              leaf_alloc 对象
     */
    LeafAlloc updateMaxIdByCustomStepAndGetLeafAlloc(LeafAlloc leafAlloc);

    /**
    * 获取所有业务标识
    * @return                   业务标识列表
    */
    List<String> getAllTags();
}
