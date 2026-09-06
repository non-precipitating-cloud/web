package com.restaurant.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.restaurant.common.Result;
import com.restaurant.entity.Category;
import com.restaurant.service.CategoryService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Resource
    private CategoryService categoryService;

    /**
     * 获取所有分类 (公开，用于下拉选择等)
     */
    @GetMapping("/list")
    public Result<List<Category>> list() {
        return Result.ok(categoryService.listAll());
    }

    /**
     * 分页查询分类 (管理员，支持名称搜索)
     */
    @GetMapping("/admin/list")
    public Result<Page<Category>> adminList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.ok(categoryService.pageQuery(pageNum, pageSize, keyword));
    }

    /**
     * 新增分类 (管理员)
     */
    @PostMapping
    public Result<?> add(@RequestBody Category category) {
        categoryService.addCategory(category.getName(), category.getSortOrder());
        return Result.ok("分类新增成功");
    }

    /**
     * 修改分类 (管理员)
     */
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @RequestBody Category category) {
        categoryService.updateCategory(id, category.getName(), category.getSortOrder());
        return Result.ok("分类修改成功");
    }

    /**
     * 删除分类 (管理员)
     */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.ok("分类删除成功");
    }
}
