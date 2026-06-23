-- Marca · 默刻 数据库建表脚本
-- 执行前请先创建 database: CREATE DATABASE marca DEFAULT CHARACTER SET utf8mb4;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  email      VARCHAR(255) NOT NULL UNIQUE,
  password   VARCHAR(255) NOT NULL,
  nickname   VARCHAR(50),
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问题题库
CREATE TABLE IF NOT EXISTS question (
  id         BIGINT PRIMARY KEY AUTO_INCREMENT,
  category   ENUM('event', 'emotion', 'future') NOT NULL,
  content    VARCHAR(255) NOT NULL,
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
