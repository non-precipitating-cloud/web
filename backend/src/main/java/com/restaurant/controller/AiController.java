package com.restaurant.controller;

import com.restaurant.common.Result;
import com.restaurant.dto.AiRecommendDTO;
import com.restaurant.service.AiService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

/**
 * AI 功能控制器
 * 集成 5 大 AI 业务功能: 智能点餐、菜品描述、情感分析、经营分析、智能客服
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Resource
    private AiService aiService;

    /**
     * AI 智能点餐推荐
     * POST /api/ai/recommend
     * Body: { budget: "30", taste: "辣", personCount: 2 }
     */
    @PostMapping("/recommend")
    public Result<Map<String, Object>> recommend(@RequestBody AiRecommendDTO dto) {
        return Result.ok(aiService.recommend(dto));
    }

    /**
     * AI 菜品描述生成
     * POST /api/ai/generate-description
     * Body: { name: "麻辣香锅", ingredients: "牛肉、藕片...", price: "28" }
     */
    @PostMapping("/generate-description")
    public Result<String> generateDescription(@RequestBody Map<String, String> params) {
        String result = aiService.generateDescription(
                params.get("name"),
                params.get("ingredients"),
                params.get("price")
        );
        return Result.ok(result);
    }

    /**
     * AI 评论情感分析 (手动触发，实际提交评价时自动调用)
     * POST /api/ai/analyze-sentiment
     * Body: { content: "味道不错但太贵", rating: 3 }
     */
    @PostMapping("/analyze-sentiment")
    public Result<Map<String, Object>> analyzeSentiment(@RequestBody Map<String, Object> params) {
        return Result.ok(aiService.analyzeSentiment(
                (String) params.get("content"),
                (Integer) params.get("rating")
        ));
    }

    /**
     * AI 经营数据分析
     * POST /api/ai/business-analysis
     * Body: { stats: {...} }
     */
    @PostMapping("/business-analysis")
    public Result<String> businessAnalysis(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) params.get("stats");
        return Result.ok(aiService.businessAnalysis(stats));
    }

    /**
     * AI 智能客服问答
     * POST /api/ai/customer-service
     * Body: { question: "营业时间是多少？" }
     */
    @PostMapping("/customer-service")
    public Result<String> customerService(@RequestBody Map<String, String> params) {
        return Result.ok(aiService.customerService(params.get("question")));
    }
}
