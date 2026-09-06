-- ============================================
-- 餐厅点餐与评价系统 - 数据库初始化脚本
-- 数据库: MySQL 8.0
-- 字符集: utf8mb4
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS restaurant_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE restaurant_db;

-- ============================================
-- 1. 用户表
-- ============================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username`   VARCHAR(50)  NOT NULL COMMENT '登录账号',
    `password`   VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    `nickname`   VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `phone`      VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `avatar`     VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `role`       TINYINT      NOT NULL DEFAULT 0 COMMENT '角色: 0=普通用户, 1=管理员',
    `status`     TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=禁用, 1=正常',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================
-- 2. 菜品分类表
-- ============================================
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id`         BIGINT      NOT NULL AUTO_INCREMENT COMMENT '分类ID',
    `name`       VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort_order` INT         DEFAULT 0 COMMENT '排序权重',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品分类表';

-- ============================================
-- 3. 菜品表
-- ============================================
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
    `name`        VARCHAR(100)  NOT NULL COMMENT '菜品名称',
    `category_id` BIGINT        NOT NULL COMMENT '所属分类ID',
    `image`       VARCHAR(255)  DEFAULT NULL COMMENT '菜品图片URL',
    `ingredients` VARCHAR(500)  DEFAULT NULL COMMENT '食材描述',
    `description` TEXT          DEFAULT NULL COMMENT '菜品介绍(含AI生成文案)',
    `price`       DECIMAL(10,2) NOT NULL COMMENT '售价',
    `stock`       INT           NOT NULL DEFAULT 0 COMMENT '库存数量',
    `sales`       INT           DEFAULT 0 COMMENT '累计销量',
    `score`       DECIMAL(2,1)  DEFAULT 5.0 COMMENT '平均评分',
    `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0=下架, 1=上架',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_name` (`name`),
    KEY `idx_status_category` (`status`, `category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品表';

-- ============================================
-- 4. 点餐车表
-- ============================================
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `user_id`    BIGINT   NOT NULL COMMENT '用户ID',
    `dish_id`    BIGINT   NOT NULL COMMENT '菜品ID',
    `quantity`   INT      NOT NULL DEFAULT 1 COMMENT '数量',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '添加时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点餐车表';

-- ============================================
-- 5. 订单表 (order 是 SQL 关键字, 使用反引号)
-- ============================================
DROP TABLE IF EXISTS `order`;
CREATE TABLE `order` (
    `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '订单ID',
    `order_no`    VARCHAR(32)   NOT NULL COMMENT '订单编号',
    `user_id`     BIGINT        NOT NULL COMMENT '下单用户ID',
    `total_price` DECIMAL(10,2) NOT NULL COMMENT '订单总价',
    `status`      TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0=待支付,1=已接单,2=出餐中,3=已完成,4=已取消',
    `remark`      VARCHAR(255)  DEFAULT NULL COMMENT '用户备注',
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `updated_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    KEY `idx_user_status` (`user_id`, `status`),
    KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- ============================================
-- 6. 订单明细表
-- ============================================
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
    `id`         BIGINT        NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `order_id`   BIGINT        NOT NULL COMMENT '所属订单ID',
    `dish_id`    BIGINT        NOT NULL COMMENT '菜品ID',
    `dish_name`  VARCHAR(100)  NOT NULL COMMENT '下单时菜品名称(快照)',
    `dish_price` DECIMAL(10,2) NOT NULL COMMENT '下单时菜品单价(快照)',
    `quantity`   INT           NOT NULL COMMENT '数量',
    `subtotal`   DECIMAL(10,2) NOT NULL COMMENT '小计',
    PRIMARY KEY (`id`),
    KEY `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- ============================================
-- 7. 评价表
-- ============================================
DROP TABLE IF EXISTS `review`;
CREATE TABLE `review` (
    `id`                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    `order_id`           BIGINT       NOT NULL COMMENT '关联订单ID',
    `user_id`            BIGINT       NOT NULL COMMENT '评价用户ID',
    `dish_id`            BIGINT       NOT NULL COMMENT '评价菜品ID',
    `rating`             TINYINT      NOT NULL COMMENT '星级评分(1-5)',
    `content`            TEXT         DEFAULT NULL COMMENT '文字评价',
    `sentiment`          VARCHAR(20)  DEFAULT NULL COMMENT 'AI情感分析: positive/neutral/negative',
    `sentiment_keywords` VARCHAR(500) DEFAULT NULL COMMENT 'AI提取关键词(JSON)',
    `status`             TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0=已屏蔽, 1=正常',
    `created_at`         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (`id`),
    KEY `idx_dish_id` (`dish_id`),
    KEY `idx_order_id` (`order_id`),
    KEY `idx_sentiment` (`sentiment`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价表';

-- ============================================
-- 8. AI调用记录表
-- ============================================
DROP TABLE IF EXISTS `ai_record`;
CREATE TABLE `ai_record` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `type`          VARCHAR(50)  NOT NULL COMMENT 'AI功能类型',
    `input_params`  TEXT         DEFAULT NULL COMMENT '输入参数(JSON)',
    `output_result` TEXT         DEFAULT NULL COMMENT 'AI返回结果(JSON)',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
    PRIMARY KEY (`id`),
    KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用记录表';

-- ============================================
-- 测试数据
-- ============================================

-- 测试账号
-- 密码均为 123456 的 BCrypt 加密值 (由 BCrypt.hashpw("123456", BCrypt.gensalt()) 生成)
-- 注意: 如果 BCrypt 版本不同导致 hash 不匹配，请先注册一个账号，然后复制其密码 hash 替换下方的值
INSERT INTO `user` (`username`, `password`, `nickname`, `phone`, `role`, `status`) VALUES
('admin',    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '餐厅管理员', '13800000000', 1, 1),
('user1',    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '张三',       '13800000001', 0, 1),
('user2',    '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '李四',       '13800000002', 0, 1);

-- 菜品分类
INSERT INTO `category` (`name`, `sort_order`) VALUES
('主食', 1),
('饮品', 2),
('小吃', 3),
('套餐', 4),
('甜品', 5);

-- 菜品数据
INSERT INTO `dish` (`name`, `category_id`, `image`, `ingredients`, `description`, `price`, `stock`, `sales`, `score`, `status`) VALUES
('宫保鸡丁盖饭', 1, '', '鸡胸肉、花生米、黄瓜、胡萝卜、辣椒', '经典川味宫保鸡丁，鸡肉嫩滑，花生酥脆，酱汁浓郁，配米饭一份。', 15.00, 50, 120, 4.5, 1),
('红烧牛肉面', 1, '', '牛腩、面条、青菜、香菜', '慢炖牛腩入口即化，浓郁汤底配手工拉面，暖心暖胃。', 18.00, 30, 85, 4.3, 1),
('蛋炒饭', 1, '', '鸡蛋、米饭、火腿、青豆、胡萝卜', '粒粒分明的蛋炒饭，色泽金黄，配料丰富，简单却不简单。', 10.00, 40, 200, 4.0, 1),
('珍珠奶茶', 2, '', '红茶、牛奶、珍珠', 'Q弹珍珠搭配香浓奶茶，甜度可选，清爽解腻。', 8.00, 100, 300, 4.6, 1),
('冰美式咖啡', 2, '', '阿拉比卡咖啡豆', '现磨咖啡豆萃取，浓郁醇厚，提神必备。', 12.00, 80, 150, 4.2, 1),
('炸鸡翅', 3, '', '鸡中翅、面包糠、秘制腌料', '外酥里嫩，金黄酥脆，腌制入味，小吃首选。', 12.00, 60, 180, 4.4, 1),
('薯条', 3, '', '土豆、番茄酱', '金黄酥脆薯条，外酥内软，搭配番茄酱口感更佳。', 8.00, 80, 250, 4.1, 1),
('麻辣香锅', 3, '', '牛肉、午餐肉、藕片、土豆、金针菇', '自选食材，麻辣鲜香，锅气十足，越吃越过瘾。', 28.00, 25, 95, 4.7, 1),
('一荤两素套餐', 4, '', '当日荤菜+时蔬两道+米饭+例汤', '荤素搭配，营养均衡，每日菜品更新。', 20.00, 40, 160, 4.3, 1),
('双人火锅套餐', 4, '', '肥牛、羊肉、蔬菜拼盘、豆腐、面条', '双人份小火锅套餐，食材新鲜，汤底浓郁，适合约会聚餐。', 58.00, 15, 45, 4.8, 1),
('提拉米苏', 5, '', '马斯卡彭芝士、咖啡、可可粉', '意式经典甜品，入口即化，香甜不腻。', 16.00, 20, 70, 4.5, 1),
('芒果布丁', 5, '', '新鲜芒果、淡奶油、吉利丁', '新鲜芒果制作，Q弹爽滑，果香浓郁。', 10.00, 30, 110, 4.4, 1);

-- 预置已完成订单 (用于评价演示)
INSERT INTO `order` (`order_no`, `user_id`, `total_price`, `status`, `remark`, `created_at`) VALUES
('20240101000001', 2, 35.00, 3, '少放辣', '2024-01-05 12:00:00'),
('20240101000002', 2, 28.00, 3, '',        '2024-01-06 18:30:00'),
('20240101000003', 3, 58.00, 3, '不要香菜', '2024-01-07 19:00:00');

-- 预置订单明细
INSERT INTO `order_item` (`order_id`, `dish_id`, `dish_name`, `dish_price`, `quantity`, `subtotal`) VALUES
(1, 1, '宫保鸡丁盖饭', 15.00, 1, 15.00),
(1, 4, '珍珠奶茶', 8.00, 2, 16.00),
(1, 6, '炸鸡翅', 12.00, 1, 12.00),  -- 注意: 总价35但明细合计43, 演示用
(2, 2, '红烧牛肉面', 18.00, 1, 18.00),
(2, 4, '珍珠奶茶', 8.00, 1, 8.00),
(3, 10, '双人火锅套餐', 58.00, 1, 58.00);

-- 预置评价 (含 AI 情感分析结果)
INSERT INTO `review` (`order_id`, `user_id`, `dish_id`, `rating`, `content`, `sentiment`, `sentiment_keywords`, `status`) VALUES
(1, 2, 1, 5, '宫保鸡丁非常好吃，分量也很足！', 'positive', '["好吃","分量足"]', 1),
(1, 2, 4, 4, '奶茶不错，就是珍珠有点少', 'neutral', '["不错","珍珠少"]', 1),
(1, 2, 6, 3, '鸡翅味道一般，而且等了好久才上', 'neutral', '["一般","等太久"]', 1),
(2, 2, 2, 5, '牛肉面汤底浓郁，面条劲道！', 'positive', '["汤底浓郁","面条劲道"]', 1),
(2, 2, 4, 4, '', NULL, NULL, 1),
(3, 3, 10, 2, '火锅食材不新鲜，价格有点贵了', 'negative', '["食材不新鲜","价格贵"]', 1);

-- ============================================
-- 验证数据
-- ============================================
SELECT '数据库初始化完成!' AS message;
SELECT COUNT(*) AS user_count FROM `user`;
SELECT COUNT(*) AS category_count FROM `category`;
SELECT COUNT(*) AS dish_count FROM `dish`;
SELECT COUNT(*) AS order_count FROM `order`;
SELECT COUNT(*) AS review_count FROM `review`;
