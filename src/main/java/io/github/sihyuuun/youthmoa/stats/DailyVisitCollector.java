package io.github.sihyuuun.youthmoa.stats;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import org.springframework.stereotype.Component;

/**
 * A6 (2026-09-16 신설) — 방문자 in-memory 카운터.
 *
 * <p>매 요청마다 DB write 하지 않고 {@link ConcurrentHashMap} 에 날짜별 누적. Scheduler 가 심야에 drain 하여 {@link
 * DailyVisitRepository} 로 UPSERT.
 *
 * <p>p50 응답 지연: {@link LongAdder} + {@link ConcurrentHashMap#computeIfAbsent} 조합으로 lock-free 누적.
 */
@Component
public class DailyVisitCollector {

  /** 날짜별 카운터. drain 시점에 remove 로 회수. */
  private final Map<LocalDate, DayCounter> counters = new ConcurrentHashMap<>();

  /** 세션 기준 유니크 방문자 집계 (같은 날짜 · 같은 sessionId 는 1회만 카운트). */
  static final class DayCounter {
    final LongAdder totalVisits = new LongAdder();
    final LongAdder authenticatedVisits = new LongAdder();
    final Map<String, Boolean> sessionSeen = new ConcurrentHashMap<>();
  }

  /**
   * 페이지 방문 기록.
   *
   * @param date KST 기준 날짜
   * @param sessionId HTTP 세션 ID (익명 세션도 서블릿 컨테이너가 생성한 ID 를 그대로 사용)
   * @param authenticated 로그인 상태 여부
   */
  public void record(LocalDate date, String sessionId, boolean authenticated) {
    if (date == null) return;
    DayCounter c = counters.computeIfAbsent(date, d -> new DayCounter());
    c.totalVisits.increment();
    if (authenticated) c.authenticatedVisits.increment();
    if (sessionId != null) {
      c.sessionSeen.putIfAbsent(sessionId, Boolean.TRUE);
    }
  }

  /** 현재 누적 스냅샷 (테스트·모니터링용). */
  public Map<LocalDate, VisitCounts> snapshot() {
    Map<LocalDate, VisitCounts> out = new HashMap<>();
    counters.forEach(
        (d, c) ->
            out.put(
                d,
                new VisitCounts(
                    c.sessionSeen.size(),
                    (int) c.totalVisits.sum(),
                    (int) c.authenticatedVisits.sum())));
    return out;
  }

  /** Scheduler 가 호출 — 해당 날짜 카운터를 회수하여 반환. 반환 후 map 에서 제거된다. */
  public VisitCounts drain(LocalDate date) {
    DayCounter c = counters.remove(date);
    if (c == null) return new VisitCounts(0, 0, 0);
    return new VisitCounts(
        c.sessionSeen.size(), (int) c.totalVisits.sum(), (int) c.authenticatedVisits.sum());
  }

  /** 누적 날짜 키 조회 (drain 배치 대상 결정용). */
  public java.util.Set<LocalDate> pendingDates() {
    return counters.keySet();
  }

  public record VisitCounts(int uniqueVisitors, int totalVisits, int authenticatedVisits) {}
}
