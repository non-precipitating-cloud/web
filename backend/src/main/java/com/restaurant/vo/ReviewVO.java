package com.restaurant.vo;

import com.restaurant.entity.Review;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评价视图对象 (含用户昵称和菜品名称)
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewVO extends Review {

    /** 评价用户昵称 */
    private String nickname;

    /** 菜品名称 */
    private String dishName;
}
