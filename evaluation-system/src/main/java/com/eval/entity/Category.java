package com.eval.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评价指标大类（德育、智育、体育、美育、劳育）
 */
@Data
@TableName("category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String categoryName;  // 大类名称
    private Integer sortOrder;
    private Integer status;       // 0停用 1启用
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
