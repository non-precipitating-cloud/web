package com.restaurant.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细表
 */
@Data
@TableName("order_item")
public class OrderItem {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属订单 ID */
    private Long orderId;

    /** 菜品 ID */
    private Long dishId;

    /** 下单时菜品名称 (快照，防止菜品后续修改影响历史订单) */
    private String dishName;

    /** 下单时菜品单价 (快照) */
    private BigDecimal dishPrice;

    /** 数量 */
    private Integer quantity;

    /** 小计 */
    private BigDecimal subtotal;
}
