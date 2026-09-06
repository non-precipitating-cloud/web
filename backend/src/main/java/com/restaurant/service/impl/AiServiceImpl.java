package com.restaurant.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.restaurant.dto.AiRecommendDTO;
import com.restaurant.entity.AiRecord;
import com.restaurant.entity.Dish;
import com.restaurant.mapper.AiRecordMapper;
import com.restaurant.mapper.DishMapper;
import com.restaurant.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 服务实现
 *
 * 支持两种模式:
 * 1. 真实 API 模式: 配置 ai.api.key 后调用大模型 API (DeepSeek / OpenAI 兼容)
 * 2. 模拟模式: 未配置 API Key 时，使用本地规则模拟 AI 反馈
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    // ===== AI 点餐助手 (页面) =====
    @Value("${ai.assistant.url:}")
    private String assistantUrl;

    @Value("${ai.assistant.key:}")
    private String assistantKey;

    @Value("${ai.assistant.model:deepseek-chat}")
    private String assistantModel;

    @Value("${ai.assistant.timeout:30000}")
    private int assistantTimeout;

    // ===== AI 智能客服 (气泡) =====
    @Value("${ai.customer.url:}")
    private String customerUrl;

    @Value("${ai.customer.key:}")
    private String customerKey;

    @Value("${ai.customer.model:deepseek-chat}")
    private String customerModel;

    @Value("${ai.customer.timeout:30000}")
    private int customerTimeout;

    @Resource
    private DishMapper dishMapper;

    @Resource
    private AiRecordMapper aiRecordMapper;

    /**
     * 是否有可用的 API
     */
    private boolean hasAnyApi() {
        return isKeyValid(assistantKey) || isKeyValid(customerKey);
    }

    private boolean isKeyValid(String key) {
        return key != null && !key.isEmpty() && !"your-key-here".equals(key)
                && !"your-api-key-here".equals(key);
    }

    // ===== 身份系统提示词 =====
    private static final String ASSISTANT_SYSTEM_PROMPT =
            "你是美味餐厅的AI点餐助手。你的任务是帮用户推荐菜品、搭配套餐、介绍菜品详情。" +
            "回答要简洁专业，语气亲切自然。用中文回复。";

    private static final String CUSTOMER_SYSTEM_PROMPT =
            "你是美味餐厅的客服人员。你热情周到，能解答用户关于营业时间、配送、优惠活动、订单等问题。" +
            "回答要友好耐心，语气温暖。用中文回复。";

    // ==================== 公开接口 ====================

    @Override
    public Map<String, Object> recommend(AiRecommendDTO dto) {
        // 零预算或零人的幽默兜底
        boolean noBudget = false;
        if (dto.getBudget() != null) {
            try {
                noBudget = new java.math.BigDecimal(dto.getBudget()).compareTo(java.math.BigDecimal.ZERO) <= 0;
            } catch (Exception ignored) {}
        }
        boolean noPeople = dto.getPersonCount() != null && dto.getPersonCount() <= 0;
        if (noBudget || noPeople) {
            Map<String, Object> joke = new LinkedHashMap<>();
            StringBuilder msg = new StringBuilder("哈哈，");
            if (noBudget && noPeople) {
                msg.append("您这是打算一个人吃霸王餐吗？🤣 要不先看看我们的免费WiFi和开水？");
            } else if (noBudget) {
                msg.append("零预算吃饭，靠的是情怀和空气吗？😄 要不我给您推荐个免费看菜谱解馋的服务？");
            } else if (noPeople) {
                msg.append("零个人吃饭，是给手机点还是给空气点？👻 难道您有隐身朋友？");
            }
            msg.append(" 说正经的，设置好预算和人数，我来给您推荐真正好吃的！");
            joke.put("reason", msg.toString());
            joke.put("dishes", new java.util.ArrayList<>());
            saveAiRecord("recommend", JSON.toJSONString(dto), JSON.toJSONString(joke));
            return joke;
        }
        Map<String, Object> result;
        if (hasAnyApi()) {
            result = callRealApi("recommend", buildRecommendPrompt(dto));
        } else {
            result = simulateRecommend(dto);
        }
        saveAiRecord("recommend", JSON.toJSONString(dto), JSON.toJSONString(result));
        return result;
    }

    @Override
    public String generateDescription(String name, String ingredients, String price) {
        String result;
        if (hasAnyApi()) {
            result = callRealApiText("description", buildDescriptionPrompt(name, ingredients, price));
        } else {
            result = simulateDescription(name, ingredients, price);
        }
        saveAiRecord("description",
                JSON.toJSONString(Map.of("name", name, "ingredients", ingredients, "price", price)),
                result);
        return result;
    }

    @Override
    public Map<String, Object> analyzeSentiment(String content, Integer rating) {
        Map<String, Object> result;
        if (hasAnyApi()) {
            result = callRealApi("sentiment", buildSentimentPrompt(content, rating));
        } else {
            result = simulateSentiment(content, rating);
        }
        saveAiRecord("sentiment",
                JSON.toJSONString(Map.of("content", content, "rating", rating)),
                JSON.toJSONString(result));
        return result;
    }

    @Override
    public String businessAnalysis(Map<String, Object> stats) {
        String result;
        if (hasAnyApi()) {
            result = callRealApiText("analysis", buildAnalysisPrompt(stats));
        } else {
            result = simulateBusinessAnalysis(stats);
        }
        saveAiRecord("analysis", JSON.toJSONString(stats), result);
        return result;
    }

    @Override
    public String customerService(String question) {
        String result;
        if (hasAnyApi()) {
            result = callRealApiText("customer_service", buildCustomerServicePrompt(question));
        } else {
            result = smartCustomerService(question);
        }
        saveAiRecord("customer_service", question, result);
        return result;
    }

    // ==================== 智能客服（模拟模式，大幅增强） ====================

    /**
     * 智能客服 — 多层级匹配，能回答更广泛的问题
     */
    private String smartCustomerService(String q) {
        if (q == null || q.trim().isEmpty()) {
            return "您好！请问有什么可以帮您的？您可以问我关于菜品、营业时间、价格、优惠等问题~ 😊";
        }

        String question = q.trim();

        // ---- Layer 1: 问候 ----
        String greeting = matchGreeting(question);
        if (greeting != null) return greeting;

        // ---- Layer 2: 感谢/夸奖 ----
        String thanks = matchThanks(question);
        if (thanks != null) return thanks;

        // ---- Layer 3: 菜单/菜品查询（查数据库） ----
        if (containsAny(question, "菜单", "菜品", "有什么",  "吃的", "好吃的", "招牌", "特色",
                "推荐", "热门", "热销", "最受欢迎", "哪些", "什么菜")) {
            return answerMenuQuery(question);
        }

        // ---- Layer 4: 具体菜品询问 ----
        if (containsAny(question, "多少钱", "价格", "怎么卖", "贵不贵", "便宜")) {
            return answerPriceQuery(question);
        }

        // ---- Layer 5: 口味/饮食偏好 ----
        if (containsAny(question, "辣", "清淡", "素食", "素菜", "肉", "减肥", "低脂", "清真", "过敏")) {
            return answerDietQuery(question);
        }

        // ---- Layer 6: 营业信息 ----
        if (containsAny(question, "营业", "开门", "关门", "几点", "时间", "什么时候")) {
            return "🕐 本店营业时间为 **10:00 - 22:00**，全天候为您服务！节假日照常营业。";
        }

        // ---- Layer 7: 支付 ----
        if (containsAny(question, "支付", "微信", "支付宝", "付款", "结账", "现金")) {
            return "💳 本店支持以下支付方式：\n"
                    + "• 微信支付\n• 支付宝\n• 银联卡\n• 到店现金支付\n\n"
                    + "在线下单可直接在结算页面选择支付方式。";
        }

        // ---- Layer 8: 配送/外卖 ----
        if (containsAny(question, "配送", "外卖", "送", "快递", "多久", "送达", "自取")) {
            return "🚀 关于配送：\n"
                    + "• 校园内**免费配送**，预计 20-30 分钟送达\n"
                    + "• 支持到店**自取**，下单后 15 分钟可取\n"
                    + "• 配送范围目前限于校园内\n"
                    + "如有特殊需求，可在订单备注中说明。";
        }

        // ---- Layer 9: 优惠/活动 ----
        if (containsAny(question, "优惠", "活动", "折扣", "便宜", "划算", "新用户", "会员", "积分", "红包")) {
            return "🎁 当前优惠活动：\n"
                    + "• 新用户**首单 9 折**\n"
                    + "• 满 ¥50 减 ¥5\n"
                    + "• 每日特价菜品请关注首页公告\n"
                    + "• 累计消费可升级会员，享更多折扣\n\n"
                    + "关注公众号获取最新活动信息！";
        }

        // ---- Layer 10: 订单相关 ----
        if (containsAny(question, "退单", "退款", "取消订单", "改", "订单", "催单", "催")) {
            return "📋 订单相关说明：\n"
                    + "• **未支付**订单可自行在订单页取消\n"
                    + "• **已支付**订单请在 5 分钟内联系商家取消\n"
                    + "• 退款将在 1-3 个工作日内原路返回\n"
                    + "• 如需修改订单，建议取消后重新下单\n\n"
                    + "客服电话：138-0000-0000";
        }

        // ---- Layer 11: 联系/地址 ----
        if (containsAny(question, "电话", "地址", "在哪", "位置", "联系", "微信", "公众号")) {
            return "📞 联系方式：\n"
                    + "• 电话：**138-0000-0000**\n"
                    + "• 地址：XX 大学第一食堂 2 楼\n"
                    + "• 公众号：搜索「美味餐厅」关注\n\n"
                    + "如有任何问题欢迎随时联系我们！";
        }

        // ---- Layer 12: 预订/排队 ----
        if (containsAny(question, "预订", "预定", "排队", "包间", "座位", "位置", "包厢")) {
            return "🏠 本店目前以在线点餐为主，暂不支持提前预订座位。\n"
                    + "但您可以在到店前提前下单，到店即可取餐，免去排队等候时间！";
        }

        // ---- Layer 13: 菜品数量/分量 ----
        if (containsAny(question, "分量", "够吃", "一个人", "几人", "大小", "大份", "小份")) {
            return answerPortionQuery(question);
        }

        // ---- Layer 14: 发票 ----
        if (containsAny(question, "发票", "开票", "报销")) {
            return "🧾 如需开具发票，请在订单完成后联系商家，提供订单号和开票信息。\n"
                    + "电子发票将在 1 个工作日内发送到您的邮箱。";
        }

        // ---- 兜底: 智能推荐 + 联系商家 ----
        return fallbackAnswer(question);
    }

    // ==================== 匹配辅助方法 ====================

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    // ---- 问候 ----
    private String matchGreeting(String q) {
        if (containsAny(q, "你好", "您好", "hi", "hello", "嗨", "在吗", "在不在")) {
            String hour = java.time.LocalTime.now().getHour() < 12 ? "上午好" :
                    java.time.LocalTime.now().getHour() < 18 ? "下午好" : "晚上好";
            return "👋 " + hour + "！欢迎光临美味餐厅~ 🍽️\n\n"
                    + "我是您的 AI 点餐助手，可以帮您：\n"
                    + "• 🔍 查找菜品、了解菜单\n"
                    + "• 🤖 根据口味和预算推荐搭配\n"
                    + "• 💰 查询价格和优惠\n"
                    + "• 📋 解答订单、配送等问题\n\n"
                    + "请问有什么可以帮您的？";
        }
        return null;
    }

    // ---- 感谢/夸奖 ----
    private String matchThanks(String q) {
        if (containsAny(q, "谢谢", "感谢", "多谢", "太棒", "很好", "不错", "赞", "厉害", "Good", "nice")) {
            String[] replies = {
                "谢谢您的肯定！😊 我们会继续努力，为您提供更好的服务~",
                "感谢支持！有好吃的记得常来哦 🍜",
                "您的满意是我们最大的动力！💪 欢迎下次光临~",
                "谢谢夸奖！有什么需要随时找我 🤖✨",
            };
            return replies[Math.abs(q.hashCode()) % replies.length];
        }
        return null;
    }

    // ---- 菜单查询（查数据库）----
    private String answerMenuQuery(String q) {
        List<Dish> all = dishMapper.selectList(null);
        List<Dish> active = all.stream()
                .filter(d -> d.getStatus() == 1)
                .collect(Collectors.toList());

        if (active.isEmpty()) {
            return "目前暂无上架菜品，请稍后再来~";
        }

        // 热门推荐
        if (containsAny(q, "推荐", "好吃", "招牌", "特色", "热门", "热销", "最受欢迎", "哪些", "有什么")) {
            List<Dish> top = active.stream()
                    .sorted((a, b) -> {
                        // 按销量+评分综合排序
                        double scoreB = b.getSales() + b.getScore().doubleValue() * 20;
                        double scoreA = a.getSales() + a.getScore().doubleValue() * 20;
                        return Double.compare(scoreB, scoreA);
                    })
                    .limit(5)
                    .collect(Collectors.toList());

            StringBuilder sb = new StringBuilder("🔥 为您推荐以下热门菜品：\n\n");
            for (int i = 0; i < top.size(); i++) {
                Dish d = top.get(i);
                sb.append(i + 1).append(". **").append(d.getName()).append("**");
                sb.append(" — ¥").append(d.getPrice());
                sb.append(" | ⭐").append(d.getScore());
                sb.append(" | 月销").append(d.getSales());
                sb.append("\n");
            }
            sb.append("\n💡 输入「AI 助手」页面可以进行智能搭配推荐哦~");
            return sb.toString();
        }

        // 全部菜单
        StringBuilder sb = new StringBuilder("📋 当前菜单 (共 " + active.size() + " 道菜)：\n\n");
        Map<Long, String> catMap = getCategoryMap();
        Map<Long, List<Dish>> grouped = active.stream()
                .collect(Collectors.groupingBy(Dish::getCategoryId));
        for (Map.Entry<Long, List<Dish>> entry : grouped.entrySet()) {
            sb.append("**").append(catMap.getOrDefault(entry.getKey(), "其他")).append("**：\n");
            for (Dish d : entry.getValue()) {
                sb.append("  • ").append(d.getName()).append(" ¥").append(d.getPrice()).append("\n");
            }
        }
        sb.append("\n点餐请前往首页浏览~");
        return sb.toString();
    }

    private Map<Long, String> getCategoryMap() {
        try {
            List<com.restaurant.entity.Category> cats =
                    dishMapper.selectList(null).stream()
                            .map(Dish::getCategoryId)
                            .distinct()
                            .collect(Collectors.toList())
                            .isEmpty() ? List.of() : List.of();
            // 简化：直接用常见分类映射
            Map<Long, String> map = new LinkedHashMap<>();
            // 从 dish 数据反查分类名
            // 这里用一个简化的方式
            return map;
        } catch (Exception e) {
            return Map.of();
        }
    }

    // ---- 价格查询 ----
    private String answerPriceQuery(String q) {
        // 尝试提取菜名
        List<Dish> all = dishMapper.selectList(null);
        for (Dish d : all) {
            if (q.contains(d.getName())) {
                return "💰 **" + d.getName() + "** 的售价为 **¥" + d.getPrice() + "**\n"
                        + "食材：" + (d.getIngredients() != null ? d.getIngredients() : "精选食材") + "\n"
                        + "评分：⭐" + d.getScore() + " | 月销：" + d.getSales();
            }
        }

        // 问人均
        if (containsAny(q, "人均", "贵", "消费", "多少钱")) {
            double avg = all.stream()
                    .filter(d -> d.getStatus() == 1)
                    .mapToDouble(d -> d.getPrice().doubleValue())
                    .average().orElse(20);
            return "💰 本店菜品价格亲民：\n"
                    + "• 人均消费约 **¥" + String.format("%.0f", avg) + "**\n"
                    + "• 最低 ¥" + String.format("%.0f", all.stream().filter(d -> d.getStatus() == 1)
                        .mapToDouble(d -> d.getPrice().doubleValue()).min().orElse(8))
                    + " 起，丰俭由人\n"
                    + "• 套餐组合更划算哦~\n\n"
                    + "输入「推荐」查看热门菜品！";
        }

        return fallbackAnswer(q);
    }

    // ---- 口味/饮食查询 ----
    private String answerDietQuery(String q) {
        List<Dish> all = dishMapper.selectList(null);
        List<Dish> active = all.stream().filter(d -> d.getStatus() == 1).collect(Collectors.toList());

        if (containsAny(q, "辣")) {
            List<Dish> spicy = active.stream()
                    .filter(d -> {
                        String s = d.getName() + d.getIngredients();
                        return s.contains("辣") || s.contains("麻辣");
                    })
                    .collect(Collectors.toList());
            if (spicy.isEmpty()) return "目前没有辣味菜品，试试其他口味？🌶️";
            StringBuilder sb = new StringBuilder("🌶️ **辣味菜品推荐**：\n\n");
            for (Dish d : spicy) {
                sb.append("• ").append(d.getName()).append(" ¥").append(d.getPrice()).append("\n");
            }
            return sb.toString();
        }

        if (containsAny(q, "清淡", "素", "素食", "减肥", "低脂")) {
            List<Dish> light = active.stream()
                    .filter(d -> {
                        String s = d.getName() + d.getIngredients();
                        return s.contains("蔬菜") || s.contains("豆腐") || s.contains("菌")
                                || s.contains("汤") || s.contains("青菜") || s.contains("沙拉")
                                || d.getName().contains("面") || d.getName().contains("饭")
                                || d.getName().contains("粥");
                    })
                    .collect(Collectors.toList());
            if (light.isEmpty()) light = active.subList(0, Math.min(3, active.size()));
            StringBuilder sb = new StringBuilder("🥬 **清淡/素食推荐**：\n\n");
            for (Dish d : light) {
                sb.append("• ").append(d.getName()).append(" ¥").append(d.getPrice()).append("\n");
            }
            return sb.toString();
        }

        if (containsAny(q, "清真")) {
            return "目前菜单中大部分菜品不含猪肉，但未做清真认证。如有特殊饮食需求，建议联系商家确认。📞 138-0000-0000";
        }

        if (containsAny(q, "过敏")) {
            return "⚠️ 如您有食物过敏，请在订单备注中说明过敏源（如花生、海鲜、牛奶等），商家会注意避开。\n"
                    + "如有严重过敏史，建议下单前电话确认：138-0000-0000";
        }

        return answerMenuQuery(q);
    }

    // ---- 分量查询 ----
    private String answerPortionQuery(String q) {
        return "🍽️ 关于份量：\n"
                + "• 单道菜一般为 **1 人份**\n"
                + "• 主食类（盖饭、炒饭、面条）分量扎实\n"
                + "• 小吃类适合作为配菜或零食\n"
                + "• 套餐通常为 **1-2 人份**\n"
                + "• 不确定时建议先点 1-2 道，不够再加~\n\n"
                + "可以去「AI 助手」页面按人数智能推荐哦！";
    }

    // ---- 兜底回答 ----
    private String fallbackAnswer(String q) {
        // 尝试模糊推荐
        if (q.length() <= 2 || containsAny(q, "?", "？", "吗", "呢", "吧", "什么")) {
            return "🤔 这个问题有点难到我了...\n\n"
                    + "您可以试试问我：\n"
                    + "• 「有什么好吃的？」- 查看热门推荐\n"
                    + "• 「营业时间」- 查看门店信息\n"
                    + "• 「有什么优惠？」- 查看最新活动\n"
                    + "• 「帮我推荐几道菜」- 智能搭配\n\n"
                    + "也可以直接联系商家：📞 138-0000-0000";
        }
        return "关于「" + q + "」，建议您直接联系商家获取最准确的答复。\n\n"
                + "📞 客服电话：138-0000-0000\n"
                + "🕐 营业时间：10:00 - 22:00\n\n"
                + "我还可以帮您：推荐菜品、查菜单、问价格、了解优惠等~";
    }

    // ==================== 模拟推荐（增强） ====================

    private Map<String, Object> simulateRecommend(AiRecommendDTO dto) {
        List<Dish> allDishes = dishMapper.selectList(null);
        List<Dish> filtered = allDishes.stream()
                .filter(d -> d.getStatus() == 1 && d.getStock() > 0)
                .collect(Collectors.toList());

        java.math.BigDecimal budgetLimit = parseBudget(dto.getBudget());
        String taste = dto.getTaste();

        // 先排除口味严重冲突的菜品（要辣别推甜，要清淡别推麻辣）
        String finalTaste = taste;
        List<Dish> tasteMatched = filtered.stream()
                .filter(d -> matchTaste(d.getIngredients(), d.getName(), finalTaste) >= -3)
                .collect(Collectors.toList());
        // 如果排除后没剩几个，退回全部（兜底）
        if (tasteMatched.size() < 3) tasteMatched = filtered;

        // 按口味匹配度 + 销量 + 评分综合排序
        tasteMatched.sort((a, b) -> {
            double scoreB = matchTaste(b.getIngredients(), b.getName(), taste)
                    + b.getSales() * 0.01
                    + b.getScore().doubleValue() * 2;
            double scoreA = matchTaste(a.getIngredients(), a.getName(), taste)
                    + a.getSales() * 0.01
                    + a.getScore().doubleValue() * 2;
            return Double.compare(scoreB, scoreA);
        });

        // 按预算挑选
        List<Dish> selected = new ArrayList<>();
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (Dish dish : tasteMatched) {
            if (selected.size() >= 8) break;
            java.math.BigDecimal newTotal = total.add(dish.getPrice());
            if (budgetLimit != null && newTotal.compareTo(budgetLimit) > 0) continue;
            selected.add(dish);
            total = newTotal;
        }

        List<Map<String, Object>> dishes = new ArrayList<>();
        for (Dish d : selected) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dishId", d.getId());
            item.put("dishName", d.getName());
            item.put("price", d.getPrice());
            item.put("image", d.getImage());
            item.put("ingredients", d.getIngredients());
            item.put("reason", getRecommendReason(d, taste));
            dishes.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dishes", dishes);
        result.put("reason", buildRecommendReason(taste, selected.size(), budgetLimit, total, dto.getPersonCount()));
        result.put("totalPrice", total);
        return result;
    }

    private java.math.BigDecimal parseBudget(String budget) {
        if (budget == null || budget.isEmpty()) return null;
        try {
            String num = budget.replaceAll("[^0-9.]", "");
            if (num.isEmpty()) return null;
            return new java.math.BigDecimal(num);
        } catch (Exception e) { return null; }
    }

    private String getRecommendReason(Dish dish, String taste) {
        StringBuilder sb = new StringBuilder();
        if (taste != null && !"不忌口".equals(taste) && matchTaste(dish.getIngredients(), dish.getName(), taste) >= 3) {
            sb.append("匹配").append(taste).append("口味");
        } else if (dish.getSales() > 80) {
            sb.append("🔥热销爆款");
        } else if (dish.getScore().doubleValue() >= 4.5) {
            sb.append("⭐高分好评");
        } else if (dish.getSales() > 30) {
            sb.append("人气之选");
        } else {
            sb.append("精选推荐");
        }
        return sb.toString();
    }

    private String buildRecommendReason(String taste, int count, java.math.BigDecimal budget,
                                         java.math.BigDecimal total, Integer personCount) {
        StringBuilder sb = new StringBuilder();
        if (taste != null && !taste.isEmpty() && !"不忌口".equals(taste)) {
            sb.append("根据您「").append(taste).append("」口味");
        } else {
            sb.append("综合热门与评分");
        }
        sb.append("，精选 ").append(count).append(" 道菜品");
        if (budget != null) {
            sb.append("，总价 ¥").append(total).append(" 在预算 ¥").append(budget).append(" 以内");
        }
        if (personCount != null && personCount > 1) {
            sb.append("，适合 ").append(personCount).append(" 人分享");
        }
        sb.append("。");
        return sb.toString();
    }

    /**
     * 口味匹配度评分：正值=匹配，负值=冲突，零=中性
     * 确保"要辣的别推甜的，要清淡的别推麻辣的"
     */
    private int matchTaste(String ingredients, String name, String taste) {
        if (taste == null || "不忌口".equals(taste)) return 1;
        int score = 0;
        String lower = (ingredients + name).toLowerCase();

        // ---- 统一检测所有口味维度的特征 ----
        boolean isSpicy   = lower.contains("辣") || lower.contains("麻辣") || lower.contains("香辣")
                         || lower.contains("酸辣") || lower.contains("辣椒") || lower.contains("花椒")
                         || lower.contains("剁椒") || lower.contains("宫保");
        boolean isSweet   = lower.contains("甜") || lower.contains("糖") || lower.contains("奶油")
                         || lower.contains("芝士") || lower.contains("可可") || lower.contains("布丁")
                         || lower.contains("提拉米苏") || lower.contains("奶茶") || lower.contains("芒果")
                         || lower.contains("草莓") || lower.contains("菠萝") || lower.contains("蜂蜜");
        boolean isLight   = lower.contains("清蒸") || lower.contains("白灼") || lower.contains("素")
                         || lower.contains("汤") || lower.contains("粥") || lower.contains("蔬菜")
                         || lower.contains("豆腐") || lower.contains("菌") || lower.contains("清淡");
        boolean isSour    = lower.contains("糖醋") || lower.contains("酸甜") || lower.contains("番茄")
                         || lower.contains("柠檬") || lower.contains("橙") || lower.contains("醋");
        boolean isHeavy   = lower.contains("炸") || lower.contains("火锅") || lower.contains("油");
        boolean isBitter  = lower.contains("咖啡") || lower.contains("苦");

        switch (taste) {
            case "辣":
                if (isSpicy) score += 8;          // 命中辣味 → 强烈推荐
                // 甜品类和清淡类完全不对味 → 强力排除
                if (isSweet) score -= 12;
                if (isLight && !isSpicy) score -= 5;
                if (isBitter) score -= 3;
                break;
            case "清淡":
                if (isLight) score += 8;
                // 麻辣重口和油炸火锅 → 强力排除
                if (isSpicy) score -= 12;
                if (isHeavy) score -= 6;
                if (isSweet && !isLight) score -= 3;
                break;
            case "酸甜":
                if (isSour || isSweet) score += 8;
                // 麻辣完全破坏酸甜口感
                if (isSpicy) score -= 12;
                if (isHeavy) score -= 5;
                if (isBitter && !isSweet) score -= 4;
                break;
        }
        return score;
    }

    // ==================== 模拟其他功能 ====================

    private String simulateDescription(String name, String ingredients, String price) {
        String[] styles = {
            "精选优质食材，%s，经过大厨精心烹制，口感层次丰富。售价仅 ¥%s，是您不可错过的美味！",
            "%s选用上等%s，搭配独家酱料，每一口都是对味蕾的犒赏。¥%s，物超所值！",
            "新鲜%s匠心制作，%s的独特风味让人回味无穷。¥%s，快来品尝吧！",
        };
        int idx = Math.abs(name.hashCode()) % styles.length;
        return String.format(styles[idx], name, ingredients, price);
    }

    private Map<String, Object> simulateSentiment(String content, Integer rating) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (rating >= 4) {
            result.put("sentiment", "positive");
            result.put("keywords", extractKeywords(content, "positive"));
        } else if (rating >= 3) {
            result.put("sentiment", "neutral");
            result.put("keywords", extractKeywords(content, "neutral"));
        } else {
            result.put("sentiment", "negative");
            result.put("keywords", extractKeywords(content, "negative"));
        }
        return result;
    }

    private String extractKeywords(String content, String sentiment) {
        List<String> words = new ArrayList<>();
        String[] posWords = {"好吃", "美味", "推荐", "满意", "不错", "喜欢", "分量足", "划算", "新鲜", "热乎", "快"};
        String[] negWords = {"贵", "慢", "少", "咸", "淡", "冷", "难吃", "差", "失望", "不新鲜", "等太久"};

        String[] pool = "positive".equals(sentiment) ? posWords :
                        "negative".equals(sentiment) ? negWords :
                        new String[]{"一般", "还行", "凑合"};

        for (String w : pool) {
            if (content != null && content.contains(w)) words.add(w);
        }
        if (words.isEmpty()) words.add(pool[0]);
        return JSON.toJSONString(words.size() > 3 ? words.subList(0, 3) : words);
    }

    private String simulateBusinessAnalysis(Map<String, Object> stats) {
        StringBuilder sb = new StringBuilder();
        sb.append("📊 **经营分析报告**\n\n");
        sb.append("根据近期数据分析：\n\n");
        sb.append("**📈 销售趋势**\n");
        sb.append("• 热销菜品建议增加备货，高峰期确保供应充足\n");
        sb.append("• 低销量菜品建议分析原因（定价/口味/展示位置）\n\n");
        sb.append("**💰 营收优化**\n");
        sb.append("• 建议推出 2-3 人套餐组合提升客单价\n");
        sb.append("• 高峰时段（11:30-13:00, 17:30-19:00）可推限时特价\n\n");
        sb.append("**⭐ 口碑管理**\n");
        sb.append("• 主动邀请满意顾客留下好评\n");
        sb.append("• 对差评及时回复并提出补偿方案\n\n");
        sb.append("**🎯 运营建议**\n");
        sb.append("• 每周更新 1-2 道新品保持新鲜感\n");
        sb.append("• 利用 AI 推荐功能提升用户体验和转化率\n\n");
        sb.append("（以上为智能分析结果，接入大模型 API 可获得更精准建议）");
        return sb.toString();
    }

    // ==================== API 调用 ====================

    private Map<String, Object> callRealApi(String type, String prompt) {
        String response = callRealApiText(type, prompt);
        try {
            return JSON.parseObject(response);
        } catch (Exception e) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("result", response);
            return result;
        }
    }

    private String callRealApiText(String type, String prompt) {
        String systemPrompt;
        String url, key, model;
        int timeout;

        if ("customer_service".equals(type)) {
            // 客服 → 用客服密钥 + 客服身份
            systemPrompt = CUSTOMER_SYSTEM_PROMPT;
            url = customerUrl;
            key = customerKey;
            model = customerModel;
            timeout = customerTimeout;
        } else {
            // 助手/推荐等 → 用助手密钥 + 助手身份
            systemPrompt = ASSISTANT_SYSTEM_PROMPT;
            url = assistantUrl;
            key = assistantKey;
            model = assistantModel;
            timeout = assistantTimeout;
        }

        return tryCallApi(type, prompt, url, key, model, timeout, systemPrompt);
    }

    /**
     * 调用单个 API，失败返回 {}
     */
    private String tryCallApi(String type, String prompt, String url, String key,
                               String model, int timeout, String systemPrompt) {
        log.info("[AI-{}] Calling {} (model={}), prompt length: {}", type, url, model, prompt.length());
        try {
            JSONObject body = new JSONObject();
            body.put("model", model);

            JSONArray messages = new JSONArray();
            JSONObject sysMsg = new JSONObject();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);

            body.put("messages", messages);

            HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .body(body.toJSONString())
                    .timeout(timeout)
                    .execute();

            if (response.getStatus() == 200) {
                JSONObject json = JSON.parseObject(response.body());
                String content = json.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content");
                log.info("[AI-{}] API response received from {}", type, url);
                return content;
            } else {
                log.error("[AI-{}] API error: status={} from {}", type, response.getStatus(), url);
                return "{}";
            }
        } catch (Exception e) {
            log.error("[AI-{}] API exception from {}: {}", type, url, e.getMessage());
            return "{}";
        }
    }

    private String buildRecommendPrompt(AiRecommendDTO dto) {
        List<Dish> dishes = dishMapper.selectList(null);
        String taste = dto.getTaste() != null ? dto.getTaste() : "不忌口";
        String tasteRule = "";
        if ("辣".equals(taste)) {
            tasteRule = "严格只推荐含辣/麻辣/香辣的菜品，绝对不要推荐甜品、奶茶、布丁、提拉米苏等甜食。";
        } else if ("清淡".equals(taste)) {
            tasteRule = "严格只推荐清淡菜品（蒸、煮、汤、蔬菜类），绝对不要推荐麻辣、油炸、火锅类重口味菜品。";
        } else if ("酸甜".equals(taste)) {
            tasteRule = "严格只推荐酸甜口味菜品（糖醋、番茄、甜品、水果类），绝对不要推荐麻辣、香辣类菜品。";
        }
        return String.format(
            "你是餐厅点餐推荐助手。用户偏好: 预算%s，口味%s，%d人。%s菜单: %s。" +
            "推荐3-6道菜，控制总价。必须严格返回如下JSON格式(不要添加任何其他文字): " +
            "{\"dishes\":[{\"dishId\":数字,\"dishName\":\"菜名\",\"price\":价格数字,\"image\":\"图片URL\",\"ingredients\":\"食材\",\"reason\":\"推荐理由\"}],\"reason\":\"总推荐理由\",\"totalPrice\":总价数字}",
            dto.getBudget() != null ? dto.getBudget() : "不限",
            taste,
            dto.getPersonCount() != null ? dto.getPersonCount() : 1,
            tasteRule,
            JSON.toJSONString(dishes)
        );
    }

    private String buildDescriptionPrompt(String name, String ingredients, String price) {
        return String.format(
            "为菜品写50-80字诱人介绍。菜名:%s，食材:%s，售价:%s。突出食材和口感。",
            name, ingredients, price
        );
    }

    private String buildSentimentPrompt(String content, Integer rating) {
        return String.format(
            "分析评价情感，提1-3个关键词。评价:%s，星级:%d。返回JSON: {sentiment:'positive/neutral/negative',keywords:[...]}",
            content, rating
        );
    }

    private String buildAnalysisPrompt(Map<String, Object> stats) {
        return "分析以下餐厅经营数据，给出3-5条经营建议: " + JSON.toJSONString(stats);
    }

    private String buildCustomerServicePrompt(String question) {
        List<Dish> active = dishMapper.selectList(null).stream()
                .filter(d -> d.getStatus() == 1)
                .collect(Collectors.toList());
        return "你是美味餐厅客服。根据菜单和知识回答用户问题。如果不知道建议联系商家(电话138-0000-0000)。" +
               "营业时间10-22点，可退单(未支付)，校园配送，微信/支付宝。当前在线菜品: " +
               JSON.toJSONString(active.stream().map(d -> Map.of(
                   "name", d.getName(), "price", d.getPrice(),
                   "sales", d.getSales(), "score", d.getScore(),
                   "ingredients", d.getIngredients() != null ? d.getIngredients() : ""
               )).collect(Collectors.toList())) +
               "\n用户问题: " + question;
    }

    private void saveAiRecord(String type, String input, String output) {
        try {
            AiRecord record = new AiRecord();
            record.setType(type);
            record.setInputParams(input);
            record.setOutputResult(output);
            aiRecordMapper.insert(record);
        } catch (Exception e) {
            log.error("Save AI record failed", e);
        }
    }
}
