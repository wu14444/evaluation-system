package com.eval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eval.entity.User;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT u.*, c.class_name FROM `user` u LEFT JOIN eval_class c ON u.class_id = c.id")
    List<Map<String,Object>> selectUserList();
}
