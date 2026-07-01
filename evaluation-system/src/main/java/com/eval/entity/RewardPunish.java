package com.eval.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("reward_punish")
public class RewardPunish {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer studentId;
    private Integer batchId;
    private Integer type;
    private String category;
    private String title;
    private String description;
    private BigDecimal scoreChange;
    private String attachment;
    private Integer status;
    private Integer auditorId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}