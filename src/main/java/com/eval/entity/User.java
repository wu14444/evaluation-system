package com.eval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户表（学生、教师、管理员）
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String studentNo;    // 学号/工号
    private String name;
    private String password;
    private Integer role;        // 0学生 1教师 2管理员
    private Integer classId;     // 班级ID（学生）
    private String phone;
    private String email;
    private Integer status;      // 0禁用 1启用
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
