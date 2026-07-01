package com.eval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 互评记录
 */
@Data
@TableName("peer_eval")
public class PeerEval {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer reviewerId;   // 评审员ID
    private Integer targetId;     // 被评学生ID
    private Integer batchId;
    private Integer indicatorId;
    private BigDecimal score;
    private String content;       // 评价内容
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
