package com.restaurant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.entity.Dish;

/**
 * 菜品服务接口
 */
public interface DishService extends IService<Dish> {

    /**
     * 分页查询菜品列表 (支持分类筛选、名称搜索、状态过滤)
     */
    Page<Dish> pageQuery(Integer pageNum, Integer pageSize,
                         Long categoryId, String keyword, Integer status);

    /**
     * 新增菜品
     */
    void addDish(Dish dish);

    /**
     * 修改菜品
     */
    void updateDish(Dish dish);

    /**
     * 上下架菜品
     */
    void changeStatus(Long id, Integer status);

    /**
     * 更新菜品评分 (评价后触发)
     */
    void updateScore(Long dishId);
}
