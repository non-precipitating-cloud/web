package com.restaurant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.restaurant.entity.Review;
import com.restaurant.vo.ReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewMapper extends BaseMapper<Review> {

    /**
     * 分页查询菜品评价 (含用户昵称)
     */
    @Select("SELECT r.*, u.nickname FROM review r " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "WHERE r.dish_id = #{dishId} AND r.status = 1 " +
            "ORDER BY r.created_at DESC")
    Page<ReviewVO> selectPageByDish(Page<ReviewVO> page, @Param("dishId") Long dishId);

    /**
     * 管理员分页查询全部评价 (含用户昵称, 可选菜品筛选)
     */
    @Select("<script>" +
            "SELECT r.*, u.nickname FROM review r " +
            "LEFT JOIN user u ON r.user_id = u.id " +
            "<where>" +
            "<if test='dishId != null'>r.dish_id = #{dishId}</if>" +
            "</where>" +
            "ORDER BY r.created_at DESC" +
            "</script>")
    Page<ReviewVO> selectPageAll(Page<ReviewVO> page, @Param("dishId") Long dishId);

    /**
     * 根据订单ID查询评价 (含菜品名称)
     */
    @Select("SELECT r.id, r.order_id, r.user_id, r.dish_id, r.rating, r.content, " +
            "r.sentiment, r.sentiment_keywords, r.status, r.created_at, " +
            "d.name AS dishName " +
            "FROM review r " +
            "LEFT JOIN dish d ON r.dish_id = d.id " +
            "WHERE r.order_id = #{orderId} " +
            "ORDER BY r.created_at ASC")
    List<ReviewVO> selectByOrderId(@Param("orderId") Long orderId);
}
