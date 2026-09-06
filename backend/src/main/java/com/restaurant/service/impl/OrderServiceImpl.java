package com.restaurant.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.entity.*;
import com.restaurant.exception.BusinessException;
import com.restaurant.mapper.*;
import com.restaurant.service.CartService;
import com.restaurant.service.OrderService;
import com.restaurant.vo.CartVO;
import com.restaurant.vo.OrderItemVO;
import com.restaurant.vo.OrderVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单服务实现
 */
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Resource
    private CartService cartService;
    @Resource
    private OrderItemMapper orderItemMapper;
    @Resource
    private DishMapper dishMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private ReviewMapper reviewMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Order submit(Long userId, String remark) {
        // 获取点餐车
        List<CartVO> cartList = cartService.listByUser(userId);
        if (cartList.isEmpty()) {
            throw new BusinessException("点餐车为空，请先添加菜品");
        }

        // 检查所有菜品状态和库存
        for (CartVO cartVO : cartList) {
            if (cartVO.getDishStatus() == 0) {
                throw new BusinessException("菜品【" + cartVO.getDishName() + "】已下架，请重新选择");
            }
            if (cartVO.getQuantity() > cartVO.getDishStock()) {
                throw new BusinessException("菜品【" + cartVO.getDishName() + "】库存不足");
            }
        }

        // 计算总价
        BigDecimal totalPrice = cartList.stream()
                .map(CartVO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 生成订单编号 (雪花ID)
        Snowflake snowflake = IdUtil.getSnowflake(1, 1);
        String orderNo = String.valueOf(snowflake.nextId());

        // 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalPrice(totalPrice);
        order.setStatus(0); // 待支付
        order.setRemark(remark);
        this.save(order);

        // 创建订单明细 + 扣减库存
        for (CartVO cartVO : cartList) {
            OrderItem item = new OrderItem();
            item.setOrderId(order.getId());
            item.setDishId(cartVO.getDishId());
            item.setDishName(cartVO.getDishName());
            item.setDishPrice(cartVO.getDishPrice());
            item.setQuantity(cartVO.getQuantity());
            item.setSubtotal(cartVO.getSubtotal());
            orderItemMapper.insert(item);

            // 扣减库存 + 增加销量
            Dish dish = dishMapper.selectById(cartVO.getDishId());
            dish.setStock(dish.getStock() - cartVO.getQuantity());
            dish.setSales(dish.getSales() + cartVO.getQuantity());
            dishMapper.updateById(dish);
        }

        // 清空点餐车
        cartService.clearByUser(userId);

        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("仅待支付订单可取消");
        }

        // 更新订单状态
        order.setStatus(4); // 已取消
        this.updateById(order);

        // 恢复库存
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        for (OrderItem item : items) {
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish != null) {
                dish.setStock(dish.getStock() + item.getQuantity());
                dish.setSales(Math.max(0, dish.getSales() - item.getQuantity()));
                dishMapper.updateById(dish);
            }
        }
    }

    @Override
    public Page<OrderVO> listByUser(Long userId, Integer pageNum, Integer pageSize, Integer status) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId);
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return convertToVO(page);
    }

    @Override
    public Page<OrderVO> listAll(Integer pageNum, Integer pageSize, Integer status,
                                  String startDate, String endDate) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Order::getStatus, status);
        }
        if (startDate != null) {
            wrapper.ge(Order::getCreatedAt, startDate);
        }
        if (endDate != null) {
            wrapper.le(Order::getCreatedAt, endDate + " 23:59:59");
        }
        wrapper.orderByDesc(Order::getCreatedAt);

        Page<Order> page = this.page(new Page<>(pageNum, pageSize), wrapper);
        return convertToVO(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Long userId, Long orderId, String method) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("仅待支付订单可支付");
        }
        order.setStatus(1); // 已接单
        this.updateById(order);
    }

    @Override
    public void updateStatus(Long orderId, Integer status) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 校验状态流转
        if (order.getStatus() == 4) {
            throw new BusinessException("已取消的订单无法修改");
        }
        if (order.getStatus() == 5) {
            throw new BusinessException("已退款的订单无法修改");
        }
        if (order.getStatus() == 6) {
            throw new BusinessException("退款审核中的订单请使用同意/拒绝操作");
        }
        order.setStatus(status);
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        // 仅已支付（已接单/出餐中/已完成）的订单可退款
        if (order.getStatus() < 1 || order.getStatus() > 3) {
            throw new BusinessException("仅已支付的订单可退款");
        }

        // 更新状态为已退款
        order.setStatus(5);
        this.updateById(order);

        // 恢复库存
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        for (OrderItem item : items) {
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish != null) {
                dish.setStock(dish.getStock() + item.getQuantity());
                dish.setSales(Math.max(0, dish.getSales() - item.getQuantity()));
                dishMapper.updateById(dish);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userRefund(Long userId, Long orderId) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        // 已接单/出餐中/已完成 可申请退款
        if (order.getStatus() < 1 || order.getStatus() > 3) {
            throw new BusinessException("该订单状态不可申请退款");
        }

        order.setRefundPrevStatus(order.getStatus());
        order.setStatus(6); // 退款审核中
        this.updateById(order);
    }

    /**
     * 管理员同意退款
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveRefund(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 6) {
            throw new BusinessException("仅退款审核中的订单可操作");
        }

        // 更新状态为已退款
        order.setStatus(5);
        order.setRefundPrevStatus(null);
        this.updateById(order);

        // 恢复库存
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        for (OrderItem item : items) {
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish != null) {
                dish.setStock(dish.getStock() + item.getQuantity());
                dish.setSales(Math.max(0, dish.getSales() - item.getQuantity()));
                dishMapper.updateById(dish);
            }
        }
    }

    /**
     * 管理员拒绝退款 (恢复至原状态)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectRefund(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 6) {
            throw new BusinessException("仅退款审核中的订单可操作");
        }
        Integer prevStatus = order.getRefundPrevStatus();
        if (prevStatus == null || prevStatus < 1 || prevStatus > 3) {
            prevStatus = 1; // 兜底：恢复为已接单
        }
        order.setStatus(prevStatus);
        order.setRefundPrevStatus(null);
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteOrder(Long userId, Long orderId) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 3 && order.getStatus() != 4 && order.getStatus() != 5) {
            throw new BusinessException("仅已完成、已取消或已退款的订单可删除");
        }
        // 删除订单明细
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        orderItemMapper.delete(wrapper);
        // 删除订单
        this.removeById(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long userId, Long orderId, Long itemId) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("仅待支付订单可修改");
        }

        // 查找订单明细
        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null || !item.getOrderId().equals(orderId)) {
            throw new BusinessException("订单明细不存在");
        }

        // 恢复库存和销量
        Dish dish = dishMapper.selectById(item.getDishId());
        if (dish != null) {
            dish.setStock(dish.getStock() + item.getQuantity());
            dish.setSales(Math.max(0, dish.getSales() - item.getQuantity()));
            dishMapper.updateById(dish);
        }

        // 删除该明细
        orderItemMapper.deleteById(itemId);

        // 重新计算订单总价
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> remaining = orderItemMapper.selectList(wrapper);

        if (remaining.isEmpty()) {
            // 没有菜品了，自动取消订单
            order.setStatus(4);
        } else {
            BigDecimal newTotal = remaining.stream()
                    .map(OrderItem::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setTotalPrice(newTotal);
        }
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateItemQuantity(Long userId, Long orderId, Long itemId, Integer quantity) {
        Order order = this.getById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            throw new BusinessException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException("仅待支付订单可修改");
        }
        if (quantity < 0) {
            throw new BusinessException("数量不能为负数");
        }

        OrderItem item = orderItemMapper.selectById(itemId);
        if (item == null || !item.getOrderId().equals(orderId)) {
            throw new BusinessException("订单明细不存在");
        }

        if (quantity == 0) {
            // 数量为 0 等同删除
            removeItem(userId, orderId, itemId);
            return;
        }

        // 检查库存
        Dish dish = dishMapper.selectById(item.getDishId());
        if (dish != null) {
            int diff = quantity - item.getQuantity();
            if (diff > 0 && dish.getStock() < diff) {
                throw new BusinessException("库存不足，剩余 " + dish.getStock() + " 份");
            }
            // 更新库存和销量
            dish.setStock(dish.getStock() - diff);
            dish.setSales(Math.max(0, dish.getSales() + diff));
            dishMapper.updateById(dish);
        }

        // 更新数量和小计
        item.setQuantity(quantity);
        item.setSubtotal(item.getDishPrice().multiply(BigDecimal.valueOf(quantity)));
        orderItemMapper.updateById(item);

        // 重新计算订单总价
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, orderId);
        List<OrderItem> remaining = orderItemMapper.selectList(wrapper);
        BigDecimal newTotal = remaining.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setTotalPrice(newTotal);
        this.updateById(order);
    }

    @Override
    public List<OrderVO> listPendingByUser(Long userId) {
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Order::getUserId, userId)
               .eq(Order::getStatus, 0)
               .orderByDesc(Order::getId);
        List<Order> orders = this.list(wrapper);
        return orders.stream().map(this::convertSingleToVO).collect(Collectors.toList());
    }

    @Override
    public OrderVO getDetail(Long orderId) {
        Order order = this.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return convertSingleToVO(order);
    }

    // ==================== 内部方法 ====================

    private Page<OrderVO> convertToVO(Page<Order> page) {
        Page<OrderVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(page.getRecords().stream()
                .map(this::convertSingleToVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    private OrderVO convertSingleToVO(Order order) {
        OrderVO vo = new OrderVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setTotalPrice(order.getTotalPrice());
        vo.setStatus(order.getStatus());
        vo.setStatusText(getStatusText(order.getStatus()));
        vo.setRemark(order.getRemark());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setUpdatedAt(order.getUpdatedAt());

        // 查询用户昵称
        User user = userMapper.selectById(order.getUserId());
        if (user != null) {
            vo.setUsername(user.getNickname());
        }

        // 查询订单明细
        LambdaQueryWrapper<OrderItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderItem::getOrderId, order.getId());
        List<OrderItem> items = orderItemMapper.selectList(wrapper);
        vo.setItems(items.stream().map(item -> {
            OrderItemVO itemVO = new OrderItemVO();
            itemVO.setItemId(item.getId());
            itemVO.setDishId(item.getDishId());
            itemVO.setDishName(item.getDishName());
            itemVO.setDishPrice(item.getDishPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setSubtotal(item.getSubtotal());

            // 查询菜品图片
            Dish dish = dishMapper.selectById(item.getDishId());
            if (dish != null) {
                itemVO.setDishImage(dish.getImage());
            }
            return itemVO;
        }).collect(Collectors.toList()));

        // 查询是否已评价
        LambdaQueryWrapper<Review> reviewWrapper = new LambdaQueryWrapper<>();
        reviewWrapper.eq(Review::getOrderId, order.getId());
        vo.setReviewed(reviewMapper.selectCount(reviewWrapper) > 0);

        return vo;
    }

    private String getStatusText(Integer status) {
        switch (status) {
            case 0: return "待支付";
            case 1: return "已接单";
            case 2: return "出餐中";
            case 3: return "已完成";
            case 4: return "已取消";
            case 5: return "已退款";
            case 6: return "退款审核中";
            default: return "未知";
        }
    }
}
