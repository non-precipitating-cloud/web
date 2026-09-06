package com.restaurant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点餐车表
 */
@Data
@TableName("cart")
public class Cart {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 菜品 ID */
    private Long dishId;

    /** 数量 */
    private Integer quantity;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
