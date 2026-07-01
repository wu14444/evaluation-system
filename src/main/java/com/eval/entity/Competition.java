package com.eval.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("competition")
public class Competition {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String name;
    private Integer type;
    private String level;
    private String organizer;
    private String awardLevel;
    private BigDecimal bonusScore;
    private Integer sortOrder;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}