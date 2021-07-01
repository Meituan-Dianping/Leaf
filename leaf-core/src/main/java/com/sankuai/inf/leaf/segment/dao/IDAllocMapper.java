package com.sankuai.inf.leaf.segment.dao;

import com.sankuai.inf.leaf.segment.model.LeafAlloc;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

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

    @Delete("<script> DELETE leaf_alloc WHERE biz_tag in " +
            "   <iterate conjunction=',' open='(' close=')' property='deleteAllocTags'>" +
            "       #deleteAllocTags[]#" +
            "   </iterate> " +
            "</script>")
    int deleteAllocTags(@Param("deleteAllocTags") List<String> deleteAllocTags);

    @Insert("<script>" +
            "INSERT INTO leaf_alloc (biz_tag, step, description)" +
            "VALUES " +
            "   <iterate conjunction=',' open='(' close=')' property='insertTOLeafAllocTagList'>" +
            "       #insertTOLeafAllocTagList[]#" +
            "   </iterate> " +
            "</script>")
    int insertTOLeafAllocTagList(@Param("insertTOLeafAllocTagList") List<String> insertTOLeafAllocTagList);
}
