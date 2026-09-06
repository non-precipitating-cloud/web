package com.restaurant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.restaurant.common.Result;
import com.restaurant.dto.ReviewSubmitDTO;
import com.restaurant.service.ReviewService;
import com.restaurant.vo.ReviewVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

/**
 * 评价控制器
 */
@RestController
@RequestMapping("/api/review")
public class ReviewController {

    @Resource
    private ReviewService reviewService;

    /**
     * 提交评价
     */
    @PostMapping("/submit")
    public Result<?> submit(@Valid @RequestBody ReviewSubmitDTO dto,
                             HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        reviewService.submit(userId, dto);
        return Result.ok("评价提交成功");
    }

    /**
     * 查看菜品评价列表 (公开)
     */
    @GetMapping("/list/{dishId}")
    public Result<Page<ReviewVO>> listByDish(
            @PathVariable Long dishId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(reviewService.listByDish(dishId, pageNum, pageSize));
    }

    /**
     * 根据订单ID查看评价列表 (当前用户)
     */
    @GetMapping("/byOrder/{orderId}")
    public Result<List<ReviewVO>> listByOrder(@PathVariable Long orderId) {
        return Result.ok(reviewService.listByOrderId(orderId));
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员查看全部评价
     */
    @GetMapping("/admin/list")
    public Result<Page<ReviewVO>> adminList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long dishId) {
        return Result.ok(reviewService.listAll(pageNum, pageSize, dishId));
    }

    /**
     * 管理员屏蔽/恢复评价
     */
    @PutMapping("/admin/{reviewId}/status")
    public Result<?> toggleStatus(@PathVariable Long reviewId,
                                   @RequestParam Integer status) {
        reviewService.toggleStatus(reviewId, status);
        return Result.ok(status == 1 ? "评价已恢复" : "评价已屏蔽");
    }
}
