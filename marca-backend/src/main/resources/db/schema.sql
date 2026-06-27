-- Marca · 默刻 数据库建表脚本
-- 执行前请先创建 database: CREATE DATABASE marca DEFAULT CHARACTER SET utf8mb4;

SET NAMES utf8mb4;

-- 用户表
-- id 由应用层生成：14位时间戳(yyyyMMddHHmmss) + 5位随机，共19位 BIGINT
CREATE TABLE IF NOT EXISTS user (
  id         BIGINT PRIMARY KEY,
  email      VARCHAR(255) NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,
  nickname   VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问题题库
-- tags JSON 示例：{"time": ["late_night"], "day": ["monday", "weekend"], "season": ["winter"]}
-- 出题时按当前时段/周几/季节匹配；前 2 道偏好匹配题，其余通用题
CREATE TABLE IF NOT EXISTS question (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  category   ENUM('event', 'emotion', 'future') NOT NULL,
  content    VARCHAR(255) NOT NULL,
  tags       JSON NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 记录主表
CREATE TABLE IF NOT EXISTS record (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,
  record_date    DATE NOT NULL,
  voice_url      VARCHAR(500),
  voice_duration INT,
  image_url      VARCHAR(500),
  free_text      TEXT,                              -- 用户主动写的自由记录（"我还想说"）
  created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_date (user_id, record_date),
  CONSTRAINT fk_record_user FOREIGN KEY (user_id) REFERENCES user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问答明细
CREATE TABLE IF NOT EXISTS record_answer (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_id   BIGINT NOT NULL,
  question_id BIGINT,
  question    VARCHAR(255) NOT NULL,
  category    ENUM('event', 'emotion', 'future'),
  answer      TEXT NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_answer_record   FOREIGN KEY (record_id)   REFERENCES record(id) ON DELETE CASCADE,
  CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 图片明细（一条 record 挂 0~N 张图）
CREATE TABLE IF NOT EXISTS record_image (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  record_id   BIGINT NOT NULL,
  url         VARCHAR(500) NOT NULL,
  width       INT,
  height      INT,
  bytes       INT,
  sort_order  INT NOT NULL DEFAULT 0,
  created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_image_record FOREIGN KEY (record_id) REFERENCES record(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
