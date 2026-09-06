package com.restaurant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.restaurant.entity.Category;
import com.restaurant.entity.Dish;
import com.restaurant.exception.BusinessException;
import com.restaurant.mapper.CategoryMapper;
import com.restaurant.mapper.DishMapper;
import com.restaurant.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * 分类服务实现
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Resource
    private DishMapper dishMapper;

    @Override
    public List<Category> listAll() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder);
        return this.list(wrapper);
    }

    @Override
    public Page<Category> pageQuery(Integer pageNum, Integer pageSize, String keyword) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Category::getName, keyword);
        }
        wrapper.orderByAsc(Category::getSortOrder);
        return this.page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void addCategory(String name, Integer sortOrder) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("分类名称不能为空");
        }
        if (name.length() > 50) {
            throw new BusinessException("分类名称不能超过50个字符");
        }

        // 检查名称唯一性
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, name);
        if (this.count(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }

        Category category = new Category();
        category.setName(name);
        category.setSortOrder(sortOrder != null ? sortOrder : 0);
        this.save(category);
    }

    @Override
    public void updateCategory(Long id, String name, Integer sortOrder) {
        Category category = this.getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        if (!StringUtils.hasText(name)) {
            throw new BusinessException("分类名称不能为空");
        }
        if (name.length() > 50) {
            throw new BusinessException("分类名称不能超过50个字符");
        }

        // 检查名称唯一性 (排除自身)
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, name).ne(Category::getId, id);
        if (this.count(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }

        category.setName(name);
        category.setSortOrder(sortOrder != null ? sortOrder : category.getSortOrder());
        this.updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        Category category = this.getById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        // 检查分类下是否有关联菜品
        LambdaQueryWrapper<Dish> dishWrapper = new LambdaQueryWrapper<>();
        dishWrapper.eq(Dish::getCategoryId, id);
        Long dishCount = dishMapper.selectCount(dishWrapper);
        if (dishCount > 0) {
            throw new BusinessException("该分类下存在 " + dishCount + " 个菜品，无法删除");
        }

        this.removeById(id);
    }
}
