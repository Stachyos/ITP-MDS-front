package org.help789.mds.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class EmailCodeService {

    // email -> code，5分钟自动过期
    private final Cache<String, String> codeCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(100_000)
            .build();

    // 简单限流：1小时窗口/邮箱，1分钟窗口/IP
    private final Cache<String, AtomicInteger> rateCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1))
            .maximumSize(200_000)
            .build();

    public void checkRateLimit(String email, String ip) {
        if (incAndGet("mail:" + email) > 5) {
            throw new IllegalStateException("发送过于频繁，请稍后再试");
        }
        // 分钟级 IP 窗口
        String minuteKey = "ip:" + ip + ":" + (System.currentTimeMillis() / 60_000);
        if (incAndGet(minuteKey) > 30) {
            throw new IllegalStateException("当前IP请求过多，请稍后再试");
        }
    }

    private int incAndGet(String key) {
        AtomicInteger c = rateCache.get(key, k -> new AtomicInteger(0));
        return c.incrementAndGet();
    }

    public String generateAndStore(String email) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        codeCache.put(email, code);
        return code;
    }

    public boolean verify(String email, String input) {
        String real = codeCache.getIfPresent(email);
        if (real != null && constantTimeEq(real, input)) {
            codeCache.invalidate(email); // 一次性消费
            return true;
        }
        return false;
    }

    private boolean constantTimeEq(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int r = 0;
        for (int i = 0; i < a.length(); i++) r |= a.charAt(i) ^ b.charAt(i);
        return r == 0;
    }
}
