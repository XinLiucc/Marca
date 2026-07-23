package app.marca.service;

import app.marca.entity.Question;
import app.marca.repository.QuestionRepository;
import app.marca.repository.RecordAnswerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class QuestionService {

    /** 前 HOOK_SIZE 道优先用场景匹配题（钩子），余下用通用题。 */
    private static final int HOOK_SIZE = 2;

    /** 去重窗口：最近这么多天内答过的题尽量不再出，池子抽干净了才允许重复。 */
    private static final int DEDUP_WINDOW_DAYS = 60;

    private final QuestionRepository questionRepository;
    private final RecordAnswerRepository recordAnswerRepository;

    /**
     * 给定 user + 日期 + 题数 + 当下时刻，按场景匹配 + 稳定随机出题。
     *
     * 算法：
     *  - 计算 context（时段 / 周几 / 季节 / 节日）
     *  - 时段池：tags.time 命中当前时段的题——固定占 1 个钩子名额，不管其他维度题库
     *    扩多大都不会被挤没
     *  - 特殊池：节日 > 周几 > 季节，按优先级只取命中的最高一档（节日题不空就只用节日题，
     *    没有节日再看周几，都没有才落到季节）——占另 1 个钩子名额
     *  - 两个池子各自先排除该用户最近 DEDUP_WINDOW_DAYS 天内已经答过的题；排除后池子
     *    不够选了就放弃排除（相当于这一档题目抽完一轮后重新开始，而不是卡死无题可出）
     *  - 钩子名额里池子本身是空的，就互相顶替，最后靠通用池兜底
     *  - 剩下的名额从通用池（时段池、特殊池之外的题）抽，同样先排除最近答过的
     *  - 全程同一 seed shuffle，保证同 (userId, date, count, hour) 内稳定
     */
    public List<Question> pickDaily(long userId, LocalDate date, int count, LocalDateTime now) {
        List<Question> all = questionRepository.findAll();
        if (all.isEmpty()) return Collections.emptyList();

        QuestionContext ctx = QuestionContext.from(now);
        long seed = seedOf(userId, date, count, ctx);
        Random rnd = new Random(seed);

        Set<Long> recentIds = new HashSet<>(
                recordAnswerRepository.findRecentQuestionIds(userId, date.minusDays(DEDUP_WINDOW_DAYS)));

        List<Question> timePool = filterByTagValue(all, "time", ctx.timeOfDay());
        List<Question> specialPool = specialPoolOf(all, ctx);
        List<Question> generalPool = generalPoolOf(all, timePool, specialPool);

        Collections.shuffle(timePool, rnd);
        Collections.shuffle(specialPool, rnd);
        Collections.shuffle(generalPool, rnd);

        Set<Long> used = new HashSet<>();
        List<Question> result = new ArrayList<>();
        int hookTarget = Math.min(HOOK_SIZE, count);

        // 1) 时段钩子：固定 1 个名额
        take(result, used, timePool, recentIds, Math.min(1, hookTarget));
        // 2) 特殊场景钩子：另 1 个名额
        take(result, used, specialPool, recentIds, hookTarget - result.size());
        // 3) 钩子名额还没凑够（某个池子是空的）：互相顶替
        take(result, used, timePool, recentIds, hookTarget - result.size());
        take(result, used, specialPool, recentIds, hookTarget - result.size());
        // 4) 极小题库兜底：通用池顶上凑满钩子名额
        take(result, used, generalPool, recentIds, hookTarget - result.size());
        // 5) 剩下名额从通用池抽
        take(result, used, generalPool, recentIds, count - result.size());
        // 6) 还不够（极小题库）：忽略去重，时段池/特殊池剩余兜底
        take(result, used, timePool, Collections.emptySet(), count - result.size());
        take(result, used, specialPool, Collections.emptySet(), count - result.size());
        return result;
    }

    /**
     * 从 pool 里按（已 shuffle 的）顺序取 n 道未使用过的题加入 result。
     * 优先跳过 recentIds；跳过之后不够 n 道，就放弃去重、直接用全部候选（允许重复）。
     */
    private void take(List<Question> result, Set<Long> used, List<Question> pool, Set<Long> recentIds, int n) {
        if (n <= 0) return;
        List<Question> candidates = new ArrayList<>();
        for (Question q : pool) {
            if (!used.contains(q.getId())) candidates.add(q);
        }
        List<Question> fresh = new ArrayList<>();
        for (Question q : candidates) {
            if (!recentIds.contains(q.getId())) fresh.add(q);
        }
        List<Question> source = fresh.size() >= n ? fresh : candidates;
        int taken = 0;
        for (Question q : source) {
            if (taken >= n) break;
            result.add(q);
            used.add(q.getId());
            taken++;
        }
    }

    /** 节日 > 周几 > 季节，只取命中的最高一档，不三个维度混着抢名额。包内可见方便单测直接验证优先级。 */
    List<Question> specialPoolOf(List<Question> all, QuestionContext ctx) {
        if (ctx.holiday() != null) {
            List<Question> holidayPool = filterByTagValue(all, "holiday", ctx.holiday());
            if (!holidayPool.isEmpty()) return holidayPool;
        }
        List<Question> dayPool = filterByDay(all, ctx);
        if (!dayPool.isEmpty()) return dayPool;
        return filterByTagValue(all, "season", ctx.season());
    }

    private List<Question> filterByDay(List<Question> all, QuestionContext ctx) {
        List<Question> result = new ArrayList<>();
        for (Question q : all) {
            List<String> day = q.getTags() == null ? null : q.getTags().get("day");
            if (day != null && (day.contains(ctx.dayOfWeek()) || day.contains(ctx.dayGroup()))) {
                result.add(q);
            }
        }
        return result;
    }

    private List<Question> filterByTagValue(List<Question> all, String key, String value) {
        List<Question> result = new ArrayList<>();
        for (Question q : all) {
            List<String> values = q.getTags() == null ? null : q.getTags().get(key);
            if (values != null && values.contains(value)) result.add(q);
        }
        return result;
    }

    /** 通用池 = 既不在时段池也不在特殊池里的题（含无 tags 的题）。 */
    private List<Question> generalPoolOf(List<Question> all, List<Question> timePool, List<Question> specialPool) {
        Set<Long> claimed = new HashSet<>();
        for (Question q : timePool) claimed.add(q.getId());
        for (Question q : specialPool) claimed.add(q.getId());
        List<Question> result = new ArrayList<>();
        for (Question q : all) {
            if (!claimed.contains(q.getId())) result.add(q);
        }
        return result;
    }

    private long seedOf(long userId, LocalDate date, int count, QuestionContext ctx) {
        // 把 ctx.timeOfDay 算进 seed：换时段会换题（沉浸感的根本）
        // 但 day / season / holiday 不算 — 它们变化慢，跟 date 已经隐含了
        String input = userId + ":" + date + ":" + count + ":" + ctx.timeOfDay();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(hash, 0, Long.BYTES).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
