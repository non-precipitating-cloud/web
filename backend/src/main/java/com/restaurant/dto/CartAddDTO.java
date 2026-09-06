package com.restaurant.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 加入点餐车请求
 */
@Data
public class CartAddDTO {

    @NotNull(message = "菜品 ID 不能为空")
    private Long dishId;

    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity;
}
