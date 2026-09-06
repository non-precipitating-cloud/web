package com.restaurant.service;

import com.restaurant.dto.AiRecommendDTO;

import java.util.Map;

/**
 * AI 服务接口
 * 集成豆包/通义千问等大模型 API
 */
public interface AiService {

    /**
     * AI 智能点餐推荐
     * @param dto 用户偏好 (预算/口味/人数)
     * @return 推荐结果 (菜品清单 + 理由)
     */
    Map<String, Object> recommend(AiRecommendDTO dto);

    /**
     * AI 菜品描述生成
     * @param name 菜品名称
     * @param ingredients 食材
     * @param price 售价
     * @return AI 生成的介绍文案
     */
    String generateDescription(String name, String ingredients, String price);

    /**
     * AI 评论情感分析
     * @param content 评价内容
     * @param rating 星级
     * @return 分析结果 {sentiment, keywords}
     */
    Map<String, Object> analyzeSentiment(String content, Integer rating);

    /**
     * AI 经营数据分析
     * @param stats 近 7/30 天订单统计数据
     * @return AI 分析建议文本
     */
    String businessAnalysis(Map<String, Object> stats);

    /**
     * AI 智能客服问答
     * @param question 用户问题
     * @return AI 回复
     */
    String customerService(String question);
}
