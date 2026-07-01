package com.eval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eval.entity.EvalClass;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface EvalClassMapper extends BaseMapper<EvalClass> {
    @Select("SELECT c.*, u.name AS advisor_name FROM eval_class c LEFT JOIN user u ON c.advisor_id = u.id")
    List<Map<String,Object>> selectClassList();
}
