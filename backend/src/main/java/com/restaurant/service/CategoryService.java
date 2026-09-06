package com.restaurant.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.restaurant.entity.Category;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService extends IService<Category> {

    /**
     * 获取所有分类 (按 sort_order 排序)
     */
    List<Category> listAll();

    /**
     * 分页查询分类 (支持名称搜索)
     */
    Page<Category> pageQuery(Integer pageNum, Integer pageSize, String keyword);

    /**
     * 新增分类
     */
    void addCategory(String name, Integer sortOrder);

    /**
     * 修改分类
     */
    void updateCategory(Long id, String name, Integer sortOrder);

    /**
     * 删除分类 (仅空分类可删)
     */
    void deleteCategory(Long id);
}
