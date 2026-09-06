package com.restaurant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.dto.ReviewSubmitDTO;
import com.restaurant.entity.Review;
import com.restaurant.vo.ReviewVO;

import java.util.List;

/**
 * 评价服务接口
 */
public interface ReviewService extends IService<Review> {

    /**
     * 用户提交评价
     */
    void submit(Long userId, ReviewSubmitDTO dto);

    /**
     * 查看菜品评价列表
     */
    Page<ReviewVO> listByDish(Long dishId, Integer pageNum, Integer pageSize);

    /**
     * 管理员查看全部评价
     */
    Page<ReviewVO> listAll(Integer pageNum, Integer pageSize, Long dishId);

    /**
     * 管理员屏蔽/恢复评价
     */
    void toggleStatus(Long reviewId, Integer status);

    /**
     * 根据订单ID查询评价列表
     */
    List<ReviewVO> listByOrderId(Long orderId);
}
