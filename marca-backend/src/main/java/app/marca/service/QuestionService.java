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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    /**
     * 给定 user + 日期 + 题数，稳定地返回 N 道问题。
     * 同一 (userId, date, count) 组合多次调用结果一致；其中任意一项变化 → 出题变化。
     */
    public List<Question> pickDaily(long userId, LocalDate date, int count) {
        List<Question> all = questionRepository.findAll();
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        long seed = seedOf(userId, date, count);
        List<Question> pool = new ArrayList<>(all);
        Collections.shuffle(pool, new Random(seed));
        return pool.subList(0, Math.min(count, pool.size()));
    }

    private long seedOf(long userId, LocalDate date, int count) {
        String input = userId + ":" + date + ":" + count;
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(hash, 0, Long.BYTES).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
