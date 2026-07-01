package com.eval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 班级表
 */
@Data
@TableName("eval_class")
public class EvalClass {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String className;
    private String grade;        // 年级 如2024级
    private String major;
    private String college;
    private Integer advisorId;   // 辅导员ID
    private Integer studentCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
