package com.restaurant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单表 (表名使用反引号转义 SQL 关键字)
 */
@Data
@TableName("`order`")
public class Order {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单编号 (雪花ID 或 UUID) */
    private String orderNo;

    /** 下单用户 ID */
    private Long userId;

    /** 订单总价 */
    private BigDecimal totalPrice;

    /**
     * 订单状态:
     * 0=待支付, 1=已接单, 2=出餐中, 3=已完成, 4=已取消, 5=已退款, 6=退款审核中
     */
    private Integer status;

    /** 申请退款前的状态 (用于审核拒绝后恢复) */
    private Integer refundPrevStatus;

    /** 用户备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
