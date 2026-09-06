package com.restaurant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.dto.ReviewSubmitDTO;
import com.restaurant.entity.Order;
import com.restaurant.entity.OrderItem;
import com.restaurant.entity.Review;
import com.restaurant.exception.BusinessException;
import com.restaurant.mapper.OrderItemMapper;
import com.restaurant.mapper.OrderMapper;
import com.restaurant.mapper.ReviewMapper;
import com.restaurant.service.AiService;
import com.restaurant.service.DishService;
import com.restaurant.service.ReviewService;
import com.restaurant.vo.ReviewVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 评价服务实现
 */
@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private DishService dishService;
    @Resource
    private AiService aiService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(Long userId, ReviewSubmitDTO dto) {
        // 检查订单是否存在且属于当前用户
        Order order = orderMapper.selectById(dto.getOrderId());
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 3) {
            throw new BusinessException("仅已完成订单可评价");
        }

        // 检查是否已评价
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getOrderId, dto.getOrderId());
        if (this.count(wrapper) > 0) {
            throw new BusinessException("该订单已评价");
        }

        // 获取订单中的菜品 (对每个菜品创建评价)
        LambdaQueryWrapper<OrderItem> itemWrapper = new LambdaQueryWrapper<>();
        itemWrapper.eq(OrderItem::getOrderId, dto.getOrderId());
        List<OrderItem> items = orderItemMapper.selectList(itemWrapper);

        for (OrderItem item : items) {
            Review review = new Review();
            review.setOrderId(dto.getOrderId());
            review.setUserId(userId);
            review.setDishId(item.getDishId());
            review.setRating(dto.getRating());
            review.setContent(dto.getContent());
            review.setStatus(1);
            this.save(review);

            // 更新菜品评分
            dishService.updateScore(item.getDishId());

            // 异步调用 AI 情感分析 (简化处理：同步调用)
            try {
                if (dto.getContent() != null && !dto.getContent().isEmpty()) {
                    // AI 情感分析
                    Map<String, Object> result = aiService.analyzeSentiment(dto.getContent(), dto.getRating());
                    review.setSentiment((String) result.get("sentiment"));
                    review.setSentimentKeywords((String) result.get("keywords"));
                    this.updateById(review);
                }
            } catch (Exception e) {
                // AI 调用失败不影响评价提交
                review.setSentiment("unknown");
                this.updateById(review);
            }
        }
    }

    @Override
    public Page<ReviewVO> listByDish(Long dishId, Integer pageNum, Integer pageSize) {
        return ((ReviewMapper) baseMapper).selectPageByDish(
                new Page<>(pageNum, pageSize), dishId);
    }

    @Override
    public Page<ReviewVO> listAll(Integer pageNum, Integer pageSize, Long dishId) {
        return ((ReviewMapper) baseMapper).selectPageAll(
                new Page<>(pageNum, pageSize), dishId);
    }

    @Override
    public void toggleStatus(Long reviewId, Integer status) {
        Review review = this.getById(reviewId);
        if (review == null) {
            throw new BusinessException("评价不存在");
        }
        review.setStatus(status);
        this.updateById(review);

        // 更新对应菜品评分
        dishService.updateScore(review.getDishId());
    }

    @Override
    public List<ReviewVO> listByOrderId(Long orderId) {
        return ((ReviewMapper) baseMapper).selectByOrderId(orderId);
    }
}
