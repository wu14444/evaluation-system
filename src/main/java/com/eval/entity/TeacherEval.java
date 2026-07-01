package com.eval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 教师评分记录
 */
@Data
@TableName("teacher_eval")
public class TeacherEval {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer teacherId;
    private Integer studentId;
    private Integer batchId;
    private Integer indicatorId;
    private BigDecimal score;
    private String comment;
    private Integer status;       // 0草稿 1已提交
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
