/*
 * Copyright 2016-2018 LEAF.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.sankuai.inf.leaf.segment.dao;

import com.sankuai.inf.leaf.segment.model.LeafAlloc;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface IDAllocMapper {

    @Select("SELECT biz_tag, max_id, step, update_time FROM leaf_alloc")
    @Results(value = {
            @Result(column = "biz_tag", property = "key"),
            @Result(column = "max_id", property = "maxId"),
            @Result(column = "step", property = "step"),
            @Result(column = "update_time", property = "updateTime")
    })
    List<LeafAlloc> getAllLeafAllocs();

    @Select("SELECT biz_tag, max_id, step FROM leaf_alloc WHERE biz_tag = #{tag}")
    @Results(value = {
            @Result(column = "biz_tag", property = "key"),
            @Result(column = "max_id", property = "maxId"),
            @Result(column = "step", property = "step")
    })
    LeafAlloc getLeafAlloc(@Param("tag") String tag);

    @Update("UPDATE leaf_alloc SET max_id = max_id + step WHERE biz_tag = #{tag}")
    void updateMaxId(@Param("tag") String tag);

    @Update("UPDATE leaf_alloc SET max_id = max_id + #{step} WHERE biz_tag = #{key}")
    void updateMaxIdByCustomStep(@Param("leafAlloc") LeafAlloc leafAlloc);

    @Select("SELECT biz_tag FROM leaf_alloc")
    List<String> getAllTags();
}
