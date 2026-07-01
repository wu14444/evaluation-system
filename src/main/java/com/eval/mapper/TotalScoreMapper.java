package com.eval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eval.entity.TotalScore;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface TotalScoreMapper extends BaseMapper<TotalScore> {
    @Select("SELECT ts.*, u.name AS student_name, u.student_no, c.class_name " +
            "FROM total_score ts LEFT JOIN user u ON ts.student_id = u.id " +
            "LEFT JOIN eval_class c ON u.class_id = c.id " +
            "WHERE ts.batch_id = #{batchId} ORDER BY ts.final_score DESC")
    List<Map<String,Object>> selectRankingByBatch(@Param("batchId") Integer batchId);

    @Select("SELECT c.class_name, AVG(ts.final_score) AS avg_score, COUNT(*) AS cnt " +
            "FROM total_score ts LEFT JOIN user u ON ts.student_id = u.id " +
            "LEFT JOIN eval_class c ON u.class_id = c.id " +
            "WHERE ts.batch_id = #{batchId} GROUP BY c.class_name ORDER BY avg_score DESC")
    List<Map<String,Object>> selectClassAvgByBatch(@Param("batchId") Integer batchId);
}
