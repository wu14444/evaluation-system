package com.eval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生自评记录
 */
@Data
@TableName("self_eval")
public class SelfEval {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer studentId;
    private Integer batchId;
    private Integer indicatorId;
    private BigDecimal selfScore;
    private String description;
    private String attachment;    // 证明材料路径
    private Integer status;       // 0暂存 1已提交
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.UPDATE)
    private LocalDateTime updateTime;
}
