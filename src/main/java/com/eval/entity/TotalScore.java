package com.eval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 综合得分汇总
 */
@Data
@TableName("total_score")
public class TotalScore {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer studentId;
    private Integer batchId;
    private BigDecimal selfTotal;
    private BigDecimal teacherTotal;
    private BigDecimal peerTotal;
    private BigDecimal bonusTotal;
    private BigDecimal finalScore;
    private Integer ranking;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
