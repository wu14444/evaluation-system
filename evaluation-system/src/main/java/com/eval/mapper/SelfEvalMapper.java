package com.eval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eval.entity.SelfEval;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface SelfEvalMapper extends BaseMapper<SelfEval> {
    @Select("SELECT se.*, i.indicator_name, i.max_score, c.category_name FROM self_eval se " +
            "LEFT JOIN indicator i ON se.indicator_id = i.id " +
            "LEFT JOIN category c ON i.category_id = c.id " +
            "WHERE se.student_id = #{studentId} AND se.batch_id = #{batchId} ORDER BY c.sort_order, i.sort_order")
    List<Map<String,Object>> selectByStudentAndBatch(@Param("studentId") Integer studentId, @Param("batchId") Integer batchId);

    @Select("SELECT COUNT(*) FROM self_eval WHERE student_id = #{studentId} AND batch_id = #{batchId} AND status = 1")
    int countSubmitted(@Param("studentId") Integer studentId, @Param("batchId") Integer batchId);
}
