package app.marca.service;

import app.marca.entity.Question;
import app.marca.repository.QuestionRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class QuestionService {

    /** 前 HOOK_SIZE 道优先用场景匹配题（钩子），余下用通用题。 */
    private static final int HOOK_SIZE = 2;

    private final QuestionRepository questionRepository;

    /**
     * 给定 user + 日期 + 题数 + 当下时刻，按场景匹配 + 稳定随机出题。
     *
     * 算法：
     *  - 计算 context（时段 / 周几 / 季节）
     *  - 池 A = tags 匹配 context 任一维度的题
     *  - 池 B = 其余（含无标签）
     *  - 前 min(HOOK_SIZE, count) 道从 A 抽（不够则 B 补）
     *  - 剩下从 B 抽
     *  - 还不够则用 A 剩余补
     *  - 全程同一 seed shuffle，保证同 (userId, date, count, hour) 稳定
     */
    public List<Question> pickDaily(long userId, LocalDate date, int count, LocalDateTime now) {
        List<Question> all = questionRepository.findAll();
        if (all.isEmpty()) return Collections.emptyList();

        QuestionContext ctx = QuestionContext.from(now);
        long seed = seedOf(userId, date, count, ctx);
        Random rnd = new Random(seed);

        List<Question> matched = new ArrayList<>();
        List<Question> general = new ArrayList<>();
        for (Question q : all) {
            if (matches(q.getTags(), ctx)) matched.add(q);
            else general.add(q);
        }
        Collections.shuffle(matched, rnd);
        Collections.shuffle(general, rnd);

        List<Question> result = new ArrayList<>();
        int hookTarget = Math.min(HOOK_SIZE, count);

        // 1) 前 hookTarget 道：优先 matched
        int matchedIdx = 0;
        while (result.size() < hookTarget && matchedIdx < matched.size()) {
            result.add(matched.get(matchedIdx++));
        }
        // 2) hook 部分如果 matched 不够，先用 general 补满
        int generalIdx = 0;
        while (result.size() < hookTarget && generalIdx < general.size()) {
            result.add(general.get(generalIdx++));
        }
        // 3) 剩下从 general 取
        while (result.size() < count && generalIdx < general.size()) {
            result.add(general.get(generalIdx++));
        }
        // 4) 还不够则用 matched 剩余兜底（极小题库 / 多匹配场景）
        while (result.size() < count && matchedIdx < matched.size()) {
            result.add(matched.get(matchedIdx++));
        }
        return result;
    }

    private boolean matches(Map<String, List<String>> tags, QuestionContext ctx) {
        if (tags == null || tags.isEmpty()) return false;
        List<String> time = tags.get("time");
        if (time != null && time.contains(ctx.timeOfDay())) return true;
        List<String> day = tags.get("day");
        if (day != null && (day.contains(ctx.dayOfWeek()) || day.contains(ctx.dayGroup()))) return true;
        List<String> season = tags.get("season");
        if (season != null && season.contains(ctx.season())) return true;
        return false;
    }

    private long seedOf(long userId, LocalDate date, int count, QuestionContext ctx) {
        // 把 ctx.timeOfDay 算进 seed：换时段会换题（沉浸感的根本）
        // 但 day / season 不算 — 它们变化慢，跟 date 已经隐含了
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
