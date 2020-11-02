package com.sankuai.inf.leaf.segment.dao;

import com.sankuai.inf.leaf.segment.model.LeafAlloc;

import java.util.List;

/**
 * leaf_alloc DAO接口
 * @author jiangyx3915
 */
public interface IDAllocDao {
    /**
     * 返回所有leaf_alloc列表
     * @return          LeafAlloc列表
     */
    List<LeafAlloc> getAllLeafAllocs();

    /**
     * 更新指定业务max_id
     * @param tag       业务标识
     * @return          更新后的leafAlloc对象
     */
    LeafAlloc updateMaxIdAndGetLeafAlloc(String tag);

    /**
     * 自定义步长更新指定业务标识的max_id
     * @param leafAlloc     leafAlloc对象
     * @return              更新后的leafAlloc对象
     */
    LeafAlloc updateMaxIdByCustomStepAndGetLeafAlloc(LeafAlloc leafAlloc);

    /**
     * 返回所有业务标识
     * @return      业务标识列表
     */
    List<String> getAllTags();
}
