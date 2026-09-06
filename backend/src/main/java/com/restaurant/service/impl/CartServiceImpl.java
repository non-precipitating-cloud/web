package com.restaurant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.entity.Cart;
import com.restaurant.entity.Dish;
import com.restaurant.exception.BusinessException;
import com.restaurant.mapper.CartMapper;
import com.restaurant.mapper.DishMapper;
import com.restaurant.service.CartService;
import com.restaurant.vo.CartVO;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 点餐车服务实现
 */
@Service
public class CartServiceImpl extends ServiceImpl<CartMapper, Cart> implements CartService {

    @Resource
    private DishMapper dishMapper;

    @Override
    public void add(Long userId, Long dishId, Integer quantity) {
        // 检查菜品是否存在且已上架
        Dish dish = dishMapper.selectById(dishId);
        if (dish == null || dish.getStatus() == 0) {
            throw new BusinessException("菜品不存在或已下架");
        }
        if (dish.getStock() < quantity) {
            throw new BusinessException("库存不足");
        }

        // 检查点餐车是否已有该菜品
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
               .eq(Cart::getDishId, dishId);
        Cart existCart = this.getOne(wrapper);

        if (existCart != null) {
            // 已有则增加数量
            int newQuantity = existCart.getQuantity() + quantity;
            if (newQuantity > dish.getStock()) {
                throw new BusinessException("超过库存上限");
            }
            existCart.setQuantity(newQuantity);
            this.updateById(existCart);
        } else {
            // 新增
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setDishId(dishId);
            cart.setQuantity(quantity);
            this.save(cart);
        }
    }

    @Override
    public void updateQuantity(Long userId, Long cartId, Integer quantity) {
        Cart cart = this.getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException("点餐车记录不存在");
        }
        if (quantity <= 0) {
            // 数量 ≤0 则删除
            this.removeById(cartId);
            return;
        }

        // 检查库存
        Dish dish = dishMapper.selectById(cart.getDishId());
        if (dish != null && quantity > dish.getStock()) {
            throw new BusinessException("超过库存上限");
        }
        cart.setQuantity(quantity);
        this.updateById(cart);
    }

    @Override
    public void remove(Long userId, Long cartId) {
        Cart cart = this.getById(cartId);
        if (cart == null || !cart.getUserId().equals(userId)) {
            throw new BusinessException("点餐车记录不存在");
        }
        this.removeById(cartId);
    }

    @Override
    public List<CartVO> listByUser(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId)
               .orderByDesc(Cart::getCreatedAt);
        List<Cart> carts = this.list(wrapper);

        return carts.stream().map(cart -> {
            Dish dish = dishMapper.selectById(cart.getDishId());
            CartVO vo = new CartVO();
            vo.setCartId(cart.getId());
            vo.setDishId(cart.getDishId());
            vo.setQuantity(cart.getQuantity());
            vo.setCreatedAt(cart.getCreatedAt());

            if (dish != null) {
                vo.setDishName(dish.getName());
                vo.setDishImage(dish.getImage());
                vo.setDishPrice(dish.getPrice());
                vo.setDishStock(dish.getStock());
                vo.setDishStatus(dish.getStatus());
                vo.setSubtotal(dish.getPrice().multiply(BigDecimal.valueOf(cart.getQuantity())));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void clearByUser(Long userId) {
        LambdaQueryWrapper<Cart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Cart::getUserId, userId);
        this.remove(wrapper);
    }
}
