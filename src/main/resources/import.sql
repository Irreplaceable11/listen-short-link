CREATE TABLE url_mappings
(
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code   VARCHAR(10) NOT NULL UNIQUE COMMENT '短码',
    original_url TEXT        NOT NULL COMMENT '原始URL',
    user_id      BIGINT COMMENT '用户ID，可为空表示匿名',
    title        VARCHAR(255) COMMENT 'URL标题',
    description  TEXT COMMENT '描述',
    domain       VARCHAR(100) DEFAULT 'short.ly' COMMENT '短链域名',
    click_count  BIGINT       DEFAULT 0 COMMENT '点击次数',
    status       TINYINT      DEFAULT 1 COMMENT '状态：1-正常，0-禁用',
    expire_time  DATETIME COMMENT '过期时间，NULL表示永不过期',
    created_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    INDEX idx_short_code (short_code),
    INDEX idx_original_url_hash (original_url(255)),
    INDEX idx_user_id (user_id),
    INDEX idx_created_time (created_time)
);

CREATE TABLE users
(
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    username         VARCHAR(50)  NOT NULL UNIQUE,
    email            VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    api_key          VARCHAR(64) UNIQUE COMMENT 'API密钥',
    quota_daily      INT      DEFAULT 1000 COMMENT '每日配额',
    quota_used_today INT      DEFAULT 0 COMMENT '今日已用配额',
    quota_reset_date DATE COMMENT '配额重置日期',
    status           TINYINT  DEFAULT 1 COMMENT '1-正常，0-禁用',
    created_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE click_statistics
(
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    short_code  VARCHAR(10) NOT NULL,
    ip_address  VARCHAR(45) COMMENT 'IPv4/IPv6地址',
    user_agent  TEXT COMMENT '用户代理',
    referer     VARCHAR(500) COMMENT '来源页面',
    country     VARCHAR(50) COMMENT '国家',
    city        VARCHAR(100) COMMENT '城市',
    device_type ENUM ('desktop', 'mobile', 'tablet', 'bot') COMMENT '设备类型',
    browser     VARCHAR(50) COMMENT '浏览器',
    os          VARCHAR(50) COMMENT '操作系统',
    click_time  DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_short_code_time (short_code, click_time),
    INDEX idx_click_time (click_time)
);

