-- 问题题库种子数据
-- 执行：mysql -u marca -p marca < seed.sql
-- 共 122 道：72 道通用基础题 + 35 道场景题 + 15 道节日题。其中 58 道带 tags（8 老题打标 + 50 新题）。
-- 2026-07-24：场景题第二轮补充，morning/afternoon/monday/friday/weekend/spring/summer/autumn 各 +2（16 道）。
-- 2026-07-24：通用题补充，PERMA(5)/叙事疗法(4)/SDT(3) 理论驱动，共 12 道，不带 tags（进通用池）。
-- 2026-07-24：节日题补充，holiday 维度从 0 到 15 个节日各 1 道。
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
('event',   '秋天有没有让你停下来的画面？', JSON_OBJECT('season', JSON_ARRAY('autumn'))),
-- afternoon 补充 x3（原来只有 1 道）
('event',   '这个下午，有没有一段时间感觉过得特别快？', JSON_OBJECT('time', JSON_ARRAY('afternoon'))),
('emotion', '这一下午犯困或走神的时候，脑子里飘过什么？', JSON_OBJECT('time', JSON_ARRAY('afternoon'))),
('future',  '这个下午有没有一件事，一直想着"待会儿再做"？', JSON_OBJECT('time', JSON_ARRAY('afternoon'))),
-- winter 补充 x3（原来是 0）
('event',   '冬天里，有没有什么让你想窝起来的时刻？', JSON_OBJECT('season', JSON_ARRAY('winter'))),
('emotion', '冷的时候，是什么让你觉得暖？', JSON_OBJECT('season', JSON_ARRAY('winter'))),
('future',  '如果给这个冬天留一句话，会是什么？', JSON_OBJECT('season', JSON_ARRAY('winter')));

-- ========== 场景题第二轮补充（8 维度各 +2，2026-07-24） ==========
INSERT INTO question (category, content, tags) VALUES
-- morning +2
('event',   '早饭吃了什么，还是没顾上吃？', JSON_OBJECT('time', JSON_ARRAY('morning'))),
('emotion', '醒来那一刻，身体是轻的还是沉的？', JSON_OBJECT('time', JSON_ARRAY('morning'))),
-- afternoon +2
('event',   '这个下午，阳光照进来的角度像什么？', JSON_OBJECT('time', JSON_ARRAY('afternoon'))),
('emotion', '一杯水、一杯咖啡，这个下午靠什么撑过去的？', JSON_OBJECT('time', JSON_ARRAY('afternoon'))),
-- monday +2
('emotion', '周一的心情，跟上周日晚上想的一样吗？', JSON_OBJECT('day', JSON_ARRAY('monday'))),
('event',   '这周的开头，发生了什么让你有点意外？', JSON_OBJECT('day', JSON_ARRAY('monday'))),
-- friday +2
('future',  '周末就要来了，最想先放下什么？', JSON_OBJECT('day', JSON_ARRAY('friday'))),
('event',   '这一周，哪一天的自己现在想起来最陌生？', JSON_OBJECT('day', JSON_ARRAY('friday'))),
-- weekend +2
('emotion', '周末的这一天，有没有真的松弛下来？', JSON_OBJECT('day', JSON_ARRAY('weekend'))),
('future',  '下周一开始前，还有什么想在今天做完？', JSON_OBJECT('day', JSON_ARRAY('weekend'))),
-- spring +2
('emotion', '春天的风吹过来，有没有让你想起点什么？', JSON_OBJECT('season', JSON_ARRAY('spring'))),
('future',  '这个春天，有没有一颗种子（不管是不是真的植物）被你悄悄种下？', JSON_OBJECT('season', JSON_ARRAY('spring'))),
-- summer +2
('event',   '夏天的这一天，热得让你记住了什么？', JSON_OBJECT('season', JSON_ARRAY('summer'))),
('emotion', '这个夏天，有没有一瞬间的凉，让你觉得很值？', JSON_OBJECT('season', JSON_ARRAY('summer'))),
-- autumn +2
('emotion', '秋天渐凉的时候，心里有没有也跟着收一收？', JSON_OBJECT('season', JSON_ARRAY('autumn'))),
('future',  '这个秋天，有什么想在冬天来之前完成？', JSON_OBJECT('season', JSON_ARRAY('autumn')));

-- ========== 通用题：PERMA / 叙事疗法 / SDT（12 道，2026-07-24） ==========
INSERT INTO question (category, content) VALUES
-- PERMA x5（积极心理学五要素）
('emotion', '今天有没有一个瞬间，纯粹地开心，不为什么原因？'),
('event',   '今天有没有一段时间，整个人投入进去，忘了看时间？'),
('event',   '今天有没有和谁的一次对话，让你觉得离得更近了？'),
('future',  '今天做的事情里，有没有哪件让你觉得"这就是我想活成的样子"？'),
('event',   '今天有没有一件小事，让你想为自己鼓个掌？'),
-- 叙事疗法 x4（外化问题 / 改写与作者身份 / 例外时刻）
('emotion', '如果把今天让你烦心的事当成故事里的一个角色，你会给它起什么名字？'),
('future',  '如果今天是你正在写的一本书的一页，你希望这一页留下什么？'),
('emotion', '今天有没有哪一刻，你不再用"一直都这样"去形容自己？'),
('future',  '如果这段经历是别人讲给你听的故事，你会想给主角一个什么样的结局？'),
-- SDT x3（自主 / 胜任 / 归属）
('event',   '今天做的事情里，哪件是你自己选的，不是被推着做的？'),
('event',   '今天有没有一件事，让你觉得"我能行"？'),
('emotion', '今天有没有一刻，让你觉得自己真的被谁看见了？');

-- ========== 节日题（15 道，holiday 维度从 0 到覆盖全部支持的节日，2026-07-24） ==========
INSERT INTO question (category, content, tags) VALUES
-- 阳历固定节日 x10
('future',  '新一年的第一天，你想悄悄许一个什么愿？', JSON_OBJECT('holiday', JSON_ARRAY('new_year'))),
('emotion', '今天有没有一份爱，被你说出口或者没说出口？', JSON_OBJECT('holiday', JSON_ARRAY('valentines_day'))),
('emotion', '今天有没有为自己做一件事，纯粹因为"我值得"？', JSON_OBJECT('holiday', JSON_ARRAY('womens_day'))),
('event',   '假期的第一天，你选择用来做什么？', JSON_OBJECT('holiday', JSON_ARRAY('labor_day'))),
('emotion', '如果今天见到小时候的自己，你想跟ta说什么？', JSON_OBJECT('holiday', JSON_ARRAY('childrens_day'))),
('emotion', '有没有一位老师，今天突然想起来？', JSON_OBJECT('holiday', JSON_ARRAY('teachers_day'))),
('event',   '长假的这一天，是在赶路还是在休息？', JSON_OBJECT('holiday', JSON_ARRAY('national_day'))),
('event',   '今天买了什么，是想要的还是顺手加的？', JSON_OBJECT('holiday', JSON_ARRAY('singles_day'))),
('emotion', '今晚有没有什么，让你觉得"这一年值了"？', JSON_OBJECT('holiday', JSON_ARRAY('christmas_eve'))),
('future',  '如果今天能收到一份礼物，你希望是什么（不一定是物品）？', JSON_OBJECT('holiday', JSON_ARRAY('christmas'))),
-- 农历节日 x5
('event',   '年夜饭桌上，有没有一句话让你记到现在？', JSON_OBJECT('holiday', JSON_ARRAY('new_year_eve'))),
('event',   '大年初一，见到的第一个人是谁？', JSON_OBJECT('holiday', JSON_ARRAY('spring_festival'))),
('emotion', '这个团圆的日子，有没有谁不在身边？', JSON_OBJECT('holiday', JSON_ARRAY('lantern_festival'))),
('event',   '今天有没有吃到一口让你想起某个人的味道？', JSON_OBJECT('holiday', JSON_ARRAY('dragon_boat_festival'))),
('emotion', '今晚的月亮，你是跟谁一起看的？', JSON_OBJECT('holiday', JSON_ARRAY('mid_autumn_festival')));
