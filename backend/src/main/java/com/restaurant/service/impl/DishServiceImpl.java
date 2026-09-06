package com.restaurant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.entity.Dish;
import com.restaurant.entity.Review;
import com.restaurant.exception.BusinessException;
import com.restaurant.mapper.DishMapper;
import com.restaurant.mapper.ReviewMapper;
import com.restaurant.service.DishService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 菜品服务实现
 */
@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {

    @Resource
    private ReviewMapper reviewMapper;

    @Override
    public Page<Dish> pageQuery(Integer pageNum, Integer pageSize,
                                 Long categoryId, String keyword, Integer status) {
        LambdaQueryWrapper<Dish> wrapper = new LambdaQueryWrapper<>();

        // 分类筛选
        if (categoryId != null && categoryId > 0) {
            wrapper.eq(Dish::getCategoryId, categoryId);
        }
        // 名称搜索
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Dish::getName, keyword);
        }
        // 状态筛选 (用户端只查上架)
        if (status != null) {
            wrapper.eq(Dish::getStatus, status);
        }
        // 按创建时间降序
        wrapper.orderByDesc(Dish::getCreatedAt);

        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void addDish(Dish dish) {
        if (dish.getPrice() == null || dish.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("价格必须大于 0");
        }
        if (dish.getStock() == null || dish.getStock() < 0) {
            throw new BusinessException("库存不能为负数");
        }
        dish.setSales(0);
        dish.setScore(BigDecimal.valueOf(5.0));
        this.save(dish);
    }

    @Override
    public void updateDish(Dish dish) {
        Dish exist = this.getById(dish.getId());
        if (exist == null) {
            throw new BusinessException("菜品不存在");
        }
        if (dish.getPrice() != null && dish.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("价格必须大于 0");
        }
        this.updateById(dish);
    }

    @Override
    public void changeStatus(Long id, Integer status) {
        Dish dish = this.getById(id);
        if (dish == null) {
            throw new BusinessException("菜品不存在");
        }
        dish.setStatus(status);
        this.updateById(dish);
    }

    @Override
    public void updateScore(Long dishId) {
        // 计算该菜品所有正常评价的平均分
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getDishId, dishId)
               .eq(Review::getStatus, 1);
        List<Review> reviews = reviewMapper.selectList(wrapper);

        if (reviews.isEmpty()) {
            return;
        }

        double avg = reviews.stream()
                .mapToInt(Review::getRating)
                .average()
                .orElse(5.0);

        BigDecimal score = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);

        LambdaUpdateWrapper<Dish> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Dish::getId, dishId)
                     .set(Dish::getScore, score);
        this.update(updateWrapper);
    }
}
