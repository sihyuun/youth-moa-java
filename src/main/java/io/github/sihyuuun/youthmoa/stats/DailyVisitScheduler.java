package io.github.sihyuuun.youthmoa.stats;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A6 (2026-09-16 신설) — DailyVisit 스케줄러.
 *
 * <p>매일 02:00 KST 에 어제 카운터를 flush + UPSERT. drain 은 어제 이전 날짜(≤ 오늘 전) 를 모두 대상으로. ({@code @Scheduled}
 * 는 재기동으로 놓친 날짜가 있을 수 있으므로.)
 *
 * <p>테스트에서는 {@code spring.task.scheduling.enabled=false} 로 비활성화.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "spring.task.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class DailyVisitScheduler {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final DailyVisitCollector collector;
  private final DailyVisitRepository repository;

  /** 매일 새벽 02:00 KST — flush 어제 이전 (오늘 카운트는 아직 진행 중이므로 제외). */
  @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
  @Transactional
  public void flushDaily() {
    LocalDate today = LocalDate.now(KST);
    Set<LocalDate> pending = new HashSet<>(collector.pendingDates());
    int flushed = 0;
    for (LocalDate d : pending) {
      if (d.isBefore(today)) {
        upsert(d);
        flushed++;
      }
    }
    if (flushed > 0) {
      log.info("[DailyVisitScheduler] flushed {} day(s)", flushed);
    }
  }

  /** 강제 flush 유틸 (테스트·admin-ops 용도). */
  @Transactional
  public void flushDate(LocalDate date) {
    upsert(date);
  }

  private void upsert(LocalDate date) {
    DailyVisitCollector.VisitCounts counts = collector.drain(date);
    if (counts.totalVisits() == 0) return;
    repository
        .findByVisitDate(date)
        .ifPresentOrElse(
            existing ->
                existing.overwrite(
                    counts.uniqueVisitors(), counts.totalVisits(), counts.authenticatedVisits()),
            () ->
                repository.save(
                    DailyVisit.builder()
                        .visitDate(date)
                        .uniqueVisitors(counts.uniqueVisitors())
                        .totalVisits(counts.totalVisits())
                        .authenticatedVisits(counts.authenticatedVisits())
                        .build()));
  }
}
