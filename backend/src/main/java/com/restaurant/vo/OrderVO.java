package com.restaurant.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情视图对象
 */
@Data
public class OrderVO {

    private Long orderId;

    private String orderNo;

    private Long userId;

    private String username;         // 下单用户昵称

    private BigDecimal totalPrice;

    private Integer status;          // 0=待支付,1=已接单,2=出餐中,3=已完成,4=已取消

    private String statusText;       // 状态中文文本

    private String remark;

    private List<OrderItemVO> items; // 订单明细

    private Boolean reviewed;        // 是否已评价

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
