package com.restaurant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 调用记录表
 */
@Data
@TableName("ai_record")
public class AiRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** AI 功能类型: recommend/description/sentiment/analysis/customer_service */
    private String type;

    /** 输入参数 (JSON) */
    private String inputParams;

    /** AI 返回结果 (JSON) */
    private String outputResult;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
