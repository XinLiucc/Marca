package app.marca.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** 19 位 BIGINT 用户 ID：yyyyMMddHHmmss(14) + 5 位随机。同秒可支持 10 万个不重复。 */
@Component
public class UserIdGenerator {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final SecureRandom RND = new SecureRandom();
    private static final int RANDOM_BOUND = 100_000;

    public long next() {
        long timestamp = Long.parseLong(LocalDateTime.now(ZONE).format(FMT));
        return timestamp * RANDOM_BOUND + RND.nextInt(RANDOM_BOUND);
    }
}
