package com.restaurant.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.entity.Cart;
import com.restaurant.vo.CartVO;

import java.util.List;

/**
 * 点餐车服务接口
 */
public interface CartService extends IService<Cart> {

    /**
     * 加入点餐车
     */
    void add(Long userId, Long dishId, Integer quantity);

    /**
     * 修改点餐车菜品数量
     */
    void updateQuantity(Long userId, Long cartId, Integer quantity);

    /**
     * 删除点餐车单品
     */
    void remove(Long userId, Long cartId);

    /**
     * 查看用户点餐车列表 (含菜品详情)
     */
    List<CartVO> listByUser(Long userId);

    /**
     * 清空用户点餐车
     */
    void clearByUser(Long userId);
}
