package io.github.sihyuuun.youthmoa.user;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * F-signup-01 Q1=(a): IP 당 send-code 1분 3회 · 1일 20회.
 *
 * <p>메모리 기반 → 서버 재기동 시 리셋. 학습 단계 요구사항엔 충분. 운영 확장 시 Redis 로 대체.
 */
@Component
public class SmsRateLimiter {

  private static final int MAX_PER_MINUTE = 3;
  private static final int MAX_PER_DAY = 20;
  private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
  private static final Duration ONE_DAY = Duration.ofDays(1);

  private final ConcurrentHashMap<String, Deque<Instant>> perIpMinute = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Deque<Instant>> perIpDay = new ConcurrentHashMap<>();

  /**
   * @return true 이면 허용. false 면 rate limit 초과.
   */
  public synchronized boolean tryAcquire(String ip) {
    Instant now = Instant.now();
    Deque<Instant> minute = perIpMinute.computeIfAbsent(ip, k -> new ArrayDeque<>());
    Deque<Instant> day = perIpDay.computeIfAbsent(ip, k -> new ArrayDeque<>());

    prune(minute, now, ONE_MINUTE);
    prune(day, now, ONE_DAY);

    if (minute.size() >= MAX_PER_MINUTE) return false;
    if (day.size() >= MAX_PER_DAY) return false;

    minute.addLast(now);
    day.addLast(now);
    return true;
  }

  private void prune(Deque<Instant> deque, Instant now, Duration window) {
    Instant cutoff = now.minus(window);
    while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
      deque.pollFirst();
    }
  }

  /** 테스트용 리셋. */
  public void reset() {
    perIpMinute.clear();
    perIpDay.clear();
  }
}
