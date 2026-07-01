package com.eval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评价批次表
 */
@Data
@TableName("batch")
public class Batch {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String academicYear;  // 学年 如2025-2026
    private String semester;      // 学期 1-第一学期 2-第二学期
    private String batchName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer status;       // 0未开始 1进行中 2已结束
    private Integer peerMode;     // 0全员互评 1采样互评
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
