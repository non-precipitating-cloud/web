package com.restaurant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.entity.Order;
import com.restaurant.vo.OrderVO;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService extends IService<Order> {

    /**
     * 提交订单 (从点餐车生成)
     */
    Order submit(Long userId, String remark);

    /**
     * 取消订单 (仅待支付状态)
     */
    void cancel(Long userId, Long orderId);

    /**
     * 用户查看自己的订单列表
     */
    Page<OrderVO> listByUser(Long userId, Integer pageNum, Integer pageSize, Integer status);

    /**
     * 管理员查看全部订单
     */
    Page<OrderVO> listAll(Integer pageNum, Integer pageSize, Integer status, String startDate, String endDate);

    /**
     * 模拟支付 (待支付 → 已接单)
     */
    void pay(Long userId, Long orderId, String method);

    /**
     * 管理员修改订单状态 (接单→出餐→完成)
     */
    void updateStatus(Long orderId, Integer status);

    /**
     * 管理员退款（已支付订单 → 已退款，恢复库存）
     */
    void refund(Long orderId);

    /**
     * 用户申请退款 (校验订单归属)
     */
    void userRefund(Long userId, Long orderId);

    /**
     * 管理员同意退款
     */
    void approveRefund(Long orderId);

    /**
     * 管理员拒绝退款 (恢复原状态)
     */
    void rejectRefund(Long orderId);

    /**
     * 删除订单 (仅已取消/已完成)
     */
    void deleteOrder(Long userId, Long orderId);

    /**
     * 移除订单中的单项菜品 (仅待支付订单)
     */
    void removeItem(Long userId, Long orderId, Long itemId);

    /**
     * 修改订单中菜品数量 (仅待支付订单)
     */
    void updateItemQuantity(Long userId, Long orderId, Long itemId, Integer quantity);

    /**
     * 查询用户待支付的订单列表
     */
    List<OrderVO> listPendingByUser(Long userId);

    /**
     * 查询订单详情
     */
    OrderVO getDetail(Long orderId);
}
