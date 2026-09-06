package com.restaurant.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 点餐车视图对象 (含菜品信息)
 */
@Data
public class CartVO {

    private Long cartId;

    private Long dishId;

    private String dishName;

    private String dishImage;

    private BigDecimal dishPrice;

    private Integer quantity;

    private BigDecimal subtotal;

    private Integer dishStock;       // 菜品库存，用于前端限制数量上限

    private Integer dishStatus;      // 菜品状态，用于判断是否已下架

    private LocalDateTime createdAt;
}
