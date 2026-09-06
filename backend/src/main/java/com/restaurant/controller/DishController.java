package com.restaurant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.restaurant.common.Result;
import com.restaurant.entity.Dish;
import com.restaurant.service.DishService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 菜品控制器
 */
@RestController
@RequestMapping("/api/dish")
public class DishController {

    @Resource
    private DishService dishService;

    /**
     * 分页查询菜品列表 (公开，仅查上架)
     */
    @GetMapping("/list")
    public Result<Page<Dish>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.ok(dishService.pageQuery(pageNum, pageSize, categoryId, keyword, 1));
    }

    /**
     * 菜品详情 (公开)
     */
    @GetMapping("/detail/{id}")
    public Result<Dish> detail(@PathVariable Long id) {
        return Result.ok(dishService.getById(id));
    }

    /**
     * 管理员 - 查询全部菜品 (含下架)
     */
    @GetMapping("/admin/list")
    public Result<Page<Dish>> adminList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.ok(dishService.pageQuery(pageNum, pageSize, categoryId, keyword, status));
    }

    /**
     * 新增菜品 (管理员)
     */
    @PostMapping
    public Result<?> add(@RequestBody Dish dish) {
        dishService.addDish(dish);
        return Result.ok("菜品新增成功");
    }

    /**
     * 修改菜品 (管理员)
     */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Dish dish) {
        dish.setId(id);
        dishService.updateDish(dish);
        return Result.ok("菜品修改成功");
    }

    /**
     * 上下架菜品 (管理员)
     */
    @PutMapping("/{id}/status")
    public Result<?> changeStatus(@PathVariable Long id,
                                   @RequestParam Integer status) {
        dishService.changeStatus(id, status);
        return Result.ok(status == 1 ? "菜品已上架" : "菜品已下架");
    }
}
