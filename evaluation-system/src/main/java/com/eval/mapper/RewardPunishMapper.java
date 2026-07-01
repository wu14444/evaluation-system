package com.eval.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eval.entity.RewardPunish;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface RewardPunishMapper extends BaseMapper<RewardPunish> {
    @Select("SELECT rp.*, u.name AS student_name FROM reward_punish rp LEFT JOIN user u ON rp.student_id = u.id ORDER BY rp.create_time DESC")
    List<Map<String,Object>> selectAllWithStudent();
}