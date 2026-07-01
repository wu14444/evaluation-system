package com.eval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eval.entity.Indicator;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface IndicatorMapper extends BaseMapper<Indicator> {
    @Select("SELECT i.*, c.category_name FROM indicator i LEFT JOIN category c ON i.category_id = c.id ORDER BY c.sort_order, i.sort_order")
    List<Map<String,Object>> selectIndicatorList();
}
