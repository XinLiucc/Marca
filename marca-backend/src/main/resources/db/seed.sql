-- 问题题库种子数据
-- 执行：mysql -u marca -p marca < seed.sql
-- 共 73 道：60 道通用基础题 + 13 道场景题。其中 21 道带 tags（8 老题打标 + 13 新题）。
-- tags 维度：time (morning/afternoon/evening/late_night) · day (monday/friday/weekend...) · season (spring/summer/autumn/winter)

SET NAMES utf8mb4;

-- ========== event 维度 ==========
INSERT INTO question (category, content) VALUES
('event', '今天有没有让你印象深刻的一件小事？'),
('event', '今天和谁说了话？说了什么让你记住？');

INSERT INTO question (category, content, tags) VALUES
('event', '今天吃了什么？哪一口让你停了一下？', JSON_OBJECT('time', JSON_ARRAY('evening', 'late_night')));

INSERT INTO question (category, content) VALUES
('event', '今天去过的地方里，最想再去一次的是哪里？'),
('event', '今天有没有什么事，原本以为很难，结果做成了？'),
('event', '今天最浪费时间的事是什么？心疼吗？'),
('event', '今天有没有人帮了你一个小忙？'),
('event', '今天遇到的最意外的事是什么？'),
('event', '今天工作 / 学习里，最有成就感的一刻？'),
('event', '今天看见的最美的画面是什么？'),
('event', '今天有没有什么事让你笑出声？'),
('event', '今天的天气怎么样？让你想起了什么？'),
('event', '今天有没有放弃做某件事？'),
('event', '今天买了什么或者想买什么？'),
('event', '今天最想抱怨的一件事是什么？'),
('event', '今天有没有什么"第一次"？'),
('event', '今天走过最长的一段路是去哪？'),
('event', '今天读到 / 看到 / 听到的最戳你的一句话是什么？'),
('event', '今天有没有哪个决定，你觉得自己挺勇敢的？'),
('event', '如果用一张照片记录今天，会是什么画面？');

-- ========== emotion 维度 ==========
INSERT INTO question (category, content) VALUES
('emotion', '此刻的你，感觉像什么？'),
('emotion', '今天有没有哪个瞬间，让你想停一停？'),
('emotion', '今天最强烈的一种情绪是什么？因为什么？');

INSERT INTO question (category, content, tags) VALUES
('emotion', '如果给今天的心情打分，1 到 10 分是多少？', JSON_OBJECT('time', JSON_ARRAY('evening', 'late_night'))),
('emotion', '今天有没有想哭，但忍住了的时刻？', NULL);

INSERT INTO question (category, content, tags) VALUES
('emotion', '今天身体有什么感觉？累、轻、紧绷、放松？', JSON_OBJECT('time', JSON_ARRAY('evening', 'late_night')));

INSERT INTO question (category, content) VALUES
('emotion', '今天有没有为谁担心？'),
('emotion', '今天有没有让你感到被理解的瞬间？'),
('emotion', '今天最让你心安的是什么？'),
('emotion', '今天有没有突然想到很久没联系的某个人？');

INSERT INTO question (category, content, tags) VALUES
('emotion', '今天最孤独的时刻是什么时候？', JSON_OBJECT('time', JSON_ARRAY('late_night')));

INSERT INTO question (category, content) VALUES
('emotion', '今天最温柔的时刻是什么？'),
('emotion', '今天有没有被什么小事治愈？'),
('emotion', '今天有没有讨厌过自己的某个瞬间？'),
('emotion', '今天最想感谢谁？为什么？'),
('emotion', '今天有没有一种情绪是你说不出名字的？'),
('emotion', '今天的能量是上升的、下降的还是平的？');

INSERT INTO question (category, content, tags) VALUES
('emotion', '今天有没有想起小时候的某个画面？', JSON_OBJECT('time', JSON_ARRAY('late_night')));

INSERT INTO question (category, content) VALUES
('emotion', '今天有没有觉得"还好我是我"？'),
('emotion', '如果今天的你是一种天气，会是什么？');

-- ========== future 维度 ==========
INSERT INTO question (category, content) VALUES
('future', '三年后的自己，会记住今天什么？'),
('future', '如果今天可以留一句话给明天，是什么？'),
('future', '今天有没有埋下一颗"以后想做"的种子？'),
('future', '十年后翻到今天，希望看到的第一行字是什么？');

INSERT INTO question (category, content, tags) VALUES
('future', '明天最想做的一件事是什么？', JSON_OBJECT('time', JSON_ARRAY('evening', 'late_night')));

INSERT INTO question (category, content) VALUES
('future', '今天的自己，让未来的自己骄傲吗？'),
('future', '今天有没有什么决定，可能会改变以后？'),
('future', '一周后回头看，今天会不会显得不一样？'),
('future', '今天有没有放下什么以前一直放不下的东西？'),
('future', '如果明天就是新的一年开始，今天会成为什么？'),
('future', '今天有没有想过"我想成为什么样的人"？'),
('future', '未来的你最想感谢今天的自己什么？'),
('future', '如果今天的自己能给一年前的自己一句话，是什么？'),
('future', '今天有没有跨出一小步？哪一步？'),
('future', '今天有没有学到一件事，你想一直记住？');

INSERT INTO question (category, content, tags) VALUES
('future', '如果今天是个章节标题，会叫什么？', JSON_OBJECT('time', JSON_ARRAY('evening', 'late_night'))),
('future', '今天结束的时候，最想跟自己说什么？', JSON_OBJECT('time', JSON_ARRAY('evening', 'late_night')));

INSERT INTO question (category, content) VALUES
('future', '今天有没有让你更确定 / 更怀疑某件事？'),
('future', '今天距离你想成为的样子，近了还是远了？'),
('future', '今天最值得被未来的你记住的一件事是什么？');

-- ========== 场景种子题（13 道，覆盖所有 tag 值） ==========
INSERT INTO question (category, content, tags) VALUES
-- morning x3
('emotion', '醒来时第一个清晰的念头是什么？', JSON_OBJECT('time', JSON_ARRAY('morning'))),
('future',  '今天想为自己留一件什么小事？', JSON_OBJECT('time', JSON_ARRAY('morning'))),
('event',   '窗外的光是什么样的？', JSON_OBJECT('time', JSON_ARRAY('morning'))),
-- afternoon x1
('event',   '这一下午，注意力被什么拉走过？', JSON_OBJECT('time', JSON_ARRAY('afternoon'))),
-- evening x2
('event',   '一天的力气，最后用在了哪？', JSON_OBJECT('time', JSON_ARRAY('evening'))),
('future',  '今晚想为自己留点什么时间？', JSON_OBJECT('time', JSON_ARRAY('evening'))),
-- late_night x1
('emotion', '这个时间还醒着，是身体撑住了，还是心里有事？', JSON_OBJECT('time', JSON_ARRAY('late_night'))),
-- day: monday / friday / weekend
('future',  '新一周的第一天，你想先解决什么？', JSON_OBJECT('day', JSON_ARRAY('monday'))),
('emotion', '这周快过完了，哪天的自己最让你意外？', JSON_OBJECT('day', JSON_ARRAY('friday'))),
('event',   '周末有没有给自己一段真正属于自己的时间？', JSON_OBJECT('day', JSON_ARRAY('weekend'))),
-- season: spring / summer (+evening dual) / autumn / winter 在 main 60 题里没有 winter 单独的，
-- 但 dev DB 里 id=65「冬天的夜里」是验算法时插的样本，可保留可删，这里不放回 seed
('event',   '春天里有没有什么"开始"，哪怕只是小事？', JSON_OBJECT('season', JSON_ARRAY('spring'))),
('future',  '这个夏天的傍晚，你最想去哪里走走？', JSON_OBJECT('season', JSON_ARRAY('summer'), 'time', JSON_ARRAY('evening'))),
('event',   '秋天有没有让你停下来的画面？', JSON_OBJECT('season', JSON_ARRAY('autumn')));
