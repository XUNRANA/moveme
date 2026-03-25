-- =============================================
-- 种子数据: 预置电影类型 + 管理员账号
-- =============================================

USE moveme;

-- 预置电影类型
INSERT INTO genres (name) VALUES
    ('剧情'), ('喜剧'), ('动作'), ('爱情'), ('科幻'),
    ('悬疑'), ('恐怖'), ('动画'), ('犯罪'), ('冒险'),
    ('奇幻'), ('战争'), ('历史'), ('传记'), ('音乐'),
    ('纪录片'), ('家庭'), ('武侠'), ('古装'), ('西部');

-- 预置管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO users (username, password, email, nickname, role, status) VALUES
    ('admin', '$2a$12$i9auj7VDf1e4SamjYQuM7eRAQm8CRfCn0OHre5MurWd4f17G./KuG', 'admin@moveme.com', '管理员', 1, 1);
