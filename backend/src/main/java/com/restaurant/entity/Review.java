package com.restaurant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评价表
 */
@Data
@TableName("review")
public class Review {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联订单 ID */
    private Long orderId;

    /** 评价用户 ID */
    private Long userId;

    /** 评价菜品 ID */
    private Long dishId;

    /** 星级评分 (1-5) */
    private Integer rating;

    /** 文字评价内容 */
    private String content;

    /** AI 情感分析结果: positive/neutral/negative */
    private String sentiment;

    /** AI 提取的关键词 (JSON 数组字符串) */
    private String sentimentKeywords;

    /** 状态: 0=已屏蔽, 1=正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
