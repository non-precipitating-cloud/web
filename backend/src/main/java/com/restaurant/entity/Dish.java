package com.restaurant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜品表
 */
@Data
@TableName("dish")
public class Dish {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 菜品名称 */
    private String name;

    /** 所属分类 ID */
    private Long categoryId;

    /** 菜品图片 URL */
    private String image;

    /** 食材描述 */
    private String ingredients;

    /** 菜品介绍 (含 AI 生成文案) */
    private String description;

    /** 售价 */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 累计销量 */
    private Integer sales;

    /** 平均评分 */
    private BigDecimal score;

    /** 状态: 0=下架, 1=上架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
