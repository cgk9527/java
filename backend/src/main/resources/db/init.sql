-- =====================================================================
-- 电商平台数据库初始化脚本 (PostgreSQL)
-- 说明: 表结构设计兼顾高并发场景
--   - 商品 SPU/SKU 分离, 库存放在 SKU 便于精细扣减
--   - 订单/订单项分离
--   - 关键查询字段建立索引
-- =====================================================================

-- ---------------------------------------------------------------------
-- 用户表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(64)  NOT NULL,
    password    VARCHAR(128) NOT NULL,           -- BCrypt 加密后的密码
    nickname    VARCHAR(64),
    phone       VARCHAR(20),
    email       VARCHAR(128),
    avatar      VARCHAR(255),
    status      SMALLINT     NOT NULL DEFAULT 1, -- 1-正常 0-禁用
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now()
);
COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.password IS 'BCrypt 加密后的密码';
CREATE UNIQUE INDEX uq_users_username ON users (username);

-- ---------------------------------------------------------------------
-- 商品分类表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS category (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(64) NOT NULL,
    parent_id   BIGINT      NOT NULL DEFAULT 0,  -- 0 表示一级分类
    level       SMALLINT    NOT NULL DEFAULT 1,
    sort        INT         NOT NULL DEFAULT 0,
    status      SMALLINT    NOT NULL DEFAULT 1,  -- 1-启用 0-禁用
    created_at  TIMESTAMP   NOT NULL DEFAULT now()
);
COMMENT ON TABLE category IS '商品分类表';
CREATE INDEX idx_category_parent ON category (parent_id);

-- ---------------------------------------------------------------------
-- 商品表(SPU)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product (
    id           BIGSERIAL PRIMARY KEY,
    category_id  BIGINT        NOT NULL,
    name         VARCHAR(255)  NOT NULL,
    subtitle     VARCHAR(255),
    main_image   VARCHAR(255),
    sub_images   TEXT,                          -- 多图, 逗号分隔
    detail       TEXT,                          -- 图文详情
    price        NUMERIC(12,2) NOT NULL,        -- 最低价/展示价
    sales        BIGINT        NOT NULL DEFAULT 0,
    status       SMALLINT      NOT NULL DEFAULT 1, -- 1-上架 0-下架
    created_at   TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP     NOT NULL DEFAULT now()
);
COMMENT ON TABLE product IS '商品表(SPU)';
CREATE INDEX idx_product_category ON product (category_id);
CREATE INDEX idx_product_status ON product (status);
CREATE INDEX idx_product_sales ON product (sales DESC);

-- ---------------------------------------------------------------------
-- SKU 表(规格库存, 高并发扣减核心表)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sku (
    id          BIGSERIAL PRIMARY KEY,
    product_id  BIGINT        NOT NULL,
    sku_code    VARCHAR(64)   NOT NULL,
    specs       VARCHAR(255),                  -- 规格描述, 如 "黑色 / 256G"
    price       NUMERIC(12,2) NOT NULL,
    stock       BIGINT        NOT NULL DEFAULT 0, -- 可用库存
    sales       BIGINT        NOT NULL DEFAULT 0,
    status      SMALLINT      NOT NULL DEFAULT 1,
    version     BIGINT        NOT NULL DEFAULT 0, -- 乐观锁版本号
    created_at  TIMESTAMP     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT now()
);
COMMENT ON TABLE sku IS 'SKU 表(规格库存)';
CREATE UNIQUE INDEX uq_sku_code ON sku (sku_code);
CREATE INDEX idx_sku_product ON sku (product_id);

-- ---------------------------------------------------------------------
-- 订单表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS orders (
    id               BIGSERIAL PRIMARY KEY,
    order_no         VARCHAR(64)   NOT NULL,
    user_id          BIGINT        NOT NULL,
    total_amount     NUMERIC(12,2) NOT NULL,
    pay_amount       NUMERIC(12,2) NOT NULL,
    status           SMALLINT      NOT NULL DEFAULT 0, -- 0-待支付 1-已支付 2-已取消 3-已关闭
    receiver_name    VARCHAR(64),
    receiver_phone   VARCHAR(20),
    receiver_address VARCHAR(255),
    remark           VARCHAR(255),
    created_at       TIMESTAMP     NOT NULL DEFAULT now(),
    paid_at          TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT now()
);
COMMENT ON TABLE orders IS '订单表';
CREATE UNIQUE INDEX uq_orders_order_no ON orders (order_no);
CREATE INDEX idx_orders_user ON orders (user_id, created_at DESC);
CREATE INDEX idx_orders_status ON orders (status);

-- ---------------------------------------------------------------------
-- 订单项表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS order_item (
    id            BIGSERIAL PRIMARY KEY,
    order_id      BIGINT        NOT NULL,
    product_id    BIGINT        NOT NULL,
    sku_id        BIGINT        NOT NULL,
    product_name  VARCHAR(255),
    sku_specs     VARCHAR(255),
    product_image VARCHAR(255),
    price         NUMERIC(12,2) NOT NULL,     -- 下单时快照单价
    quantity      INT           NOT NULL,
    total_amount  NUMERIC(12,2) NOT NULL
);
COMMENT ON TABLE order_item IS '订单项表(快照)';
CREATE INDEX idx_order_item_order ON order_item (order_id);

-- ---------------------------------------------------------------------
-- 支付流水表
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS payment (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT        NOT NULL,
    order_no    VARCHAR(64)   NOT NULL,
    user_id     BIGINT        NOT NULL,
    pay_type    SMALLINT      NOT NULL DEFAULT 1, -- 1-余额 2-支付宝 3-微信(演示)
    amount      NUMERIC(12,2) NOT NULL,
    status      SMALLINT      NOT NULL DEFAULT 0, -- 0-待支付 1-成功 2-失败
    pay_time    TIMESTAMP,
    created_at  TIMESTAMP     NOT NULL DEFAULT now()
);
COMMENT ON TABLE payment IS '支付流水表';
CREATE INDEX idx_payment_order ON payment (order_id);

-- =====================================================================
-- 种子数据(演示)
-- =====================================================================

-- 分类
INSERT INTO category (name, parent_id, level, sort) VALUES
('手机数码', 0, 1, 1),
('电脑办公', 0, 1, 2),
('家用电器', 0, 1, 3),
('服饰鞋包', 0, 1, 4)
ON CONFLICT DO NOTHING;

-- 商品
INSERT INTO product (category_id, name, subtitle, main_image, sub_images, detail, price, sales, status) VALUES
(1, '旗舰智能手机 Pro Max', '240Hz 电竞屏 · 1亿像素', 'https://trae-api-cn.mchost.guru/api/ide/v1/tool_text_to_image?prompt=a%20modern%20smartphone%20product%20photo%20on%20white%20background&image_size=square', NULL, '高性能旗舰手机, 畅快体验。', 4999.00, 1200, 1),
(2, '轻薄商务笔记本 14 英寸', '2.8K 高色域 · 长续航', 'https://trae-api-cn.mchost.guru/api/ide/v1/tool_text_to_image?prompt=a%20slim%20laptop%20product%20photo%20on%20white%20background&image_size=square', NULL, '轻薄便携, 办公利器。', 5999.00, 800, 1),
(3, '智能变频空调 1.5 匹', '一级能效 · 静音', 'https://trae-api-cn.mchost.guru/api/ide/v1/tool_text_to_image?prompt=an%20air%20conditioner%20product%20photo%20on%20white%20background&image_size=square', NULL, '节能静音, 舒适冷暖。', 2599.00, 500, 1),
(4, '透气运动跑步鞋', '轻质缓震 · 防滑耐磨', 'https://trae-api-cn.mchost.guru/api/ide/v1/tool_text_to_image?prompt=a%20pair%20of%20running%20shoes%20product%20photo%20on%20white%20background&image_size=square', NULL, '轻盈回弹, 舒适透气。', 399.00, 3000, 1)
ON CONFLICT DO NOTHING;

-- SKU(演示商品各 1 个默认规格)
INSERT INTO sku (product_id, sku_code, specs, price, stock, sales, status) VALUES
(1, 'PHONE-001-BLACK', '曜石黑 / 256G', 4999.00, 500, 1200, 1),
(2, 'LAPTOP-001-GRAY', '深空灰 / 16G+512G', 5999.00, 300, 800, 1),
(3, 'AC-001-WHITE', '皓月白 / 1.5匹', 2599.00, 200, 500, 1),
(4, 'SHOE-001-40', '黑白 / 40码', 399.00, 1000, 3000, 1)
ON CONFLICT DO NOTHING;