package com.eval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eval.entity.TeacherEval;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface TeacherEvalMapper extends BaseMapper<TeacherEval> {
    @Select("SELECT te.*, i.indicator_name, i.max_score, c.category_name, u.name AS student_name " +
            "FROM teacher_eval te LEFT JOIN indicator i ON te.indicator_id = i.id " +
            "LEFT JOIN category c ON i.category_id = c.id " +
            "LEFT JOIN user u ON te.student_id = u.id " +
            "WHERE te.teacher_id = #{teacherId} AND te.batch_id = #{batchId} " +
            "ORDER BY te.student_id, c.sort_order, i.sort_order")
    List<Map<String,Object>> selectByTeacherAndBatch(@Param("teacherId") Integer teacherId, @Param("batchId") Integer batchId);

    @Select("SELECT DISTINCT u.id, u.name, u.student_no " +
            "FROM user u WHERE u.role = 0 AND u.class_id IN " +
            "(SELECT id FROM eval_class WHERE advisor_id = #{teacherId})")
    List<Map<String,Object>> selectMyStudents(@Param("teacherId") Integer teacherId);
}
