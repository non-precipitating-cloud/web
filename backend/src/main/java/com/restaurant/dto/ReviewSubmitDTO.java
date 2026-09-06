package com.restaurant.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 评价提交请求
 */
@Data
public class ReviewSubmitDTO {

    @NotNull(message = "订单 ID 不能为空")
    private Long orderId;

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    private Integer rating;

    /** 文字评价 (可选) */
    private String content;
}
