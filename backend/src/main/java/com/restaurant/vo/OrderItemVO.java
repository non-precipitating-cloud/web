package com.restaurant.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 订单明细视图对象
 */
@Data
public class OrderItemVO {

    private Long itemId;

    private Long dishId;

    private String dishName;

    private String dishImage;

    private BigDecimal dishPrice;

    private Integer quantity;

    private BigDecimal subtotal;
}
