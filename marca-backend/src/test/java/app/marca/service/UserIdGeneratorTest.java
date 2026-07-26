package app.marca.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserIdGeneratorTest {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final long RANDOM_BOUND = 100_000L;

    private final UserIdGenerator generator = new UserIdGenerator();

    @Test
    void next_timestampPrefixMatchesCurrentTime() {
        long before = Long.parseLong(LocalDateTime.now(ZONE).format(FMT));
        long id = generator.next();
        long after = Long.parseLong(LocalDateTime.now(ZONE).format(FMT));

        long timestampPart = id / RANDOM_BOUND;
        long randomPart = id % RANDOM_BOUND;

        // 生成前后各取一次当前时间戳，真正的时间戳部分必然落在这个区间内（哪怕跨了一秒边界）
        assertTrue(timestampPart >= before && timestampPart <= after,
                "时间戳部分应该落在调用前后的区间内");
        assertTrue(randomPart >= 0 && randomPart < RANDOM_BOUND, "随机部分应该是5位数以内");
    }

    @Test
    void next_calledRepeatedly_producesDistinctIds() {
        // 调用次数少（5次），碰撞概率极低（约 5*4/2/100000 ≈ 0.01%），
        // 用来验证「正常情况下不重复」，不追求数学上绝对不重复
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            ids.add(generator.next());
        }
        assertEquals(5, ids.size());
    }
}
