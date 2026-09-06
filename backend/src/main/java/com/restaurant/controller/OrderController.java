package com.restaurant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.restaurant.common.Result;
import com.restaurant.service.OrderService;
import com.restaurant.vo.OrderVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Resource
    private OrderService orderService;

    /**
     * 提交订单 (从点餐车结算)
     */
    @PostMapping("/submit")
    public Result<Long> submit(@RequestBody Map<String, String> params,
                                HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String remark = params.getOrDefault("remark", "");
        Long orderId = orderService.submit(userId, remark).getId();
        return Result.ok("下单成功", orderId);
    }

    /**
     * 模拟支付
     */
    @PostMapping("/{orderId}/pay")
    public Result<?> pay(@PathVariable Long orderId,
                         @RequestBody Map<String, String> params,
                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String method = params.getOrDefault("method", "微信支付");
        orderService.pay(userId, orderId, method);
        return Result.ok("支付成功");
    }

    /**
     * 取消订单
     */
    @PutMapping("/{orderId}/cancel")
    public Result<?> cancel(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.cancel(userId, orderId);
        return Result.ok("订单已取消");
    }

    /**
     * 用户查看自己的订单列表
     */
    @GetMapping("/list")
    public Result<Page<OrderVO>> listByUser(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(orderService.listByUser(userId, pageNum, pageSize, status));
    }

    /**
     * 订单详情
     */
    @GetMapping("/detail/{orderId}")
    public Result<OrderVO> detail(@PathVariable Long orderId) {
        return Result.ok(orderService.getDetail(orderId));
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员查看全部订单
     */
    @GetMapping("/admin/list")
    public Result<Page<OrderVO>> adminList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.ok(orderService.listAll(pageNum, pageSize, status, startDate, endDate));
    }

    /**
     * 移除订单单项 (仅待支付)
     */
    @DeleteMapping("/{orderId}/item/{itemId}")
    public Result<?> removeItem(@PathVariable Long orderId,
                                 @PathVariable Long itemId,
                                 HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.removeItem(userId, orderId, itemId);
        return Result.ok("菜品已移除");
    }

    /**
     * 修改订单菜品数量 (仅待支付)
     */
    @PutMapping("/{orderId}/item/{itemId}/quantity")
    public Result<?> updateItemQuantity(@PathVariable Long orderId,
                                         @PathVariable Long itemId,
                                         @RequestBody Map<String, Integer> params,
                                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.updateItemQuantity(userId, orderId, itemId, params.get("quantity"));
        return Result.ok("数量已更新");
    }

    /**
     * 用户待支付订单列表 (购物车页展示)
     */
    @GetMapping("/pending")
    public Result<List<OrderVO>> pendingOrders(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(orderService.listPendingByUser(userId));
    }

    /**
     * 用户删除自己的订单 (仅已取消/已完成)
     */
    @DeleteMapping("/{orderId}")
    public Result<?> deleteOrder(@PathVariable Long orderId,
                                  HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.deleteOrder(userId, orderId);
        return Result.ok("订单已删除");
    }

    /**
     * 管理员修改订单状态
     */
    @PutMapping("/{orderId}/status")
    public Result<?> updateStatus(@PathVariable Long orderId,
                                   @RequestParam Integer status) {
        orderService.updateStatus(orderId, status);
        return Result.ok("订单状态已更新");
    }

    /**
     * 管理员退款
     */
    @PutMapping("/{orderId}/refund")
    public Result<?> refund(@PathVariable Long orderId) {
        orderService.refund(orderId);
        return Result.ok("退款成功");
    }

    /**
     * 用户申请退款
     */
    @PutMapping("/{orderId}/userRefund")
    public Result<?> userRefund(@PathVariable Long orderId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        orderService.userRefund(userId, orderId);
        return Result.ok("退款申请已提交，等待管理员审核");
    }

    /**
     * 管理员同意退款
     */
    @PutMapping("/{orderId}/approveRefund")
    public Result<?> approveRefund(@PathVariable Long orderId) {
        orderService.approveRefund(orderId);
        return Result.ok("已同意退款");
    }

    /**
     * 管理员拒绝退款
     */
    @PutMapping("/{orderId}/rejectRefund")
    public Result<?> rejectRefund(@PathVariable Long orderId) {
        orderService.rejectRefund(orderId);
        return Result.ok("已拒绝退款，订单恢复原状态");
    }
}
