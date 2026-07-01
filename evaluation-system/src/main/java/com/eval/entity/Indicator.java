package com.eval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 评价指标项
 */
@Data
@TableName("indicator")
public class Indicator {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer categoryId;   // 所属大类
    private String indicatorName; // 指标名称
    private BigDecimal maxScore;  // 满分值
    private BigDecimal weight;    // 权重
    private String scoringStandard; // 评分标准
    private Integer sortOrder;
    private Integer status;       // 0停用 1启用
    private Integer evalType;     // 0自评 1教师评 2互评 3通用
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
