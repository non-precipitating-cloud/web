package com.restaurant.dto;

import lombok.Data;

/**
 * AI 智能点餐推荐请求
 */
@Data
public class AiRecommendDTO {

    /** 预算范围 */
    private String budget;

    /** 口味偏好: 辣/清淡/酸甜/不忌口 */
    private String taste;

    /** 就餐人数 */
    private Integer personCount;
}
