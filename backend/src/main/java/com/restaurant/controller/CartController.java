package com.restaurant.controller;

import com.restaurant.common.Result;
import com.restaurant.dto.CartAddDTO;
import com.restaurant.service.CartService;
import com.restaurant.vo.CartVO;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 点餐车控制器
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Resource
    private CartService cartService;

    /**
     * 查看点餐车
     */
    @GetMapping("/list")
    public Result<List<CartVO>> list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.ok(cartService.listByUser(userId));
    }

    /**
     * 加入点餐车
     */
    @PostMapping("/add")
    public Result<?> add(@Valid @RequestBody CartAddDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cartService.add(userId, dto.getDishId(), dto.getQuantity());
        return Result.ok("已加入点餐车");
    }

    /**
     * 修改数量
     */
    @PutMapping("/{cartId}")
    public Result<?> updateQuantity(@PathVariable Long cartId,
                                     @RequestBody Map<String, Integer> params,
                                     HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cartService.updateQuantity(userId, cartId, params.get("quantity"));
        return Result.ok("数量已更新");
    }

    /**
     * 删除单品
     */
    @DeleteMapping("/{cartId}")
    public Result<?> remove(@PathVariable Long cartId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cartService.remove(userId, cartId);
        return Result.ok("已从点餐车移除");
    }

    /**
     * 清空点餐车
     */
    @DeleteMapping("/clear")
    public Result<?> clear(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        cartService.clearByUser(userId);
        return Result.ok("点餐车已清空");
    }
}
