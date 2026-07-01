package com.eval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eval.entity.PeerEval;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface PeerEvalMapper extends BaseMapper<PeerEval> {
    @Select("SELECT pe.*, i.indicator_name, u.name AS target_name FROM peer_eval pe " +
            "LEFT JOIN indicator i ON pe.indicator_id = i.id " +
            "LEFT JOIN user u ON pe.target_id = u.id " +
            "WHERE pe.reviewer_id = #{reviewerId} AND pe.batch_id = #{batchId}")
    List<Map<String,Object>> selectByReviewerAndBatch(@Param("reviewerId") Integer reviewerId, @Param("batchId") Integer batchId);
}
