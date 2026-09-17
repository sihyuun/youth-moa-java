package io.github.sihyuuun.youthmoa.stats;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A6 (2026-09-16 신설) — 일별 방문자 집계.
 *
 * <p>매 요청 DB write 는 p50 응답 악화 위험이 있어 {@code VisitTrackingInterceptor} 가 in-memory 로 누적하고 {@code
 * DailyVisitScheduler} 가 매일 02:00 KST 에 flush + UPSERT 한다.
 *
 * <p>V17__create_daily_visit.sql 과 세트.
 */
@Getter
@Entity
@Table(name = "daily_visit")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyVisit extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "visit_date", nullable = false, unique = true)
  private LocalDate visitDate;

  @Column(name = "unique_visitors", nullable = false)
  private int uniqueVisitors;

  @Column(name = "total_visits", nullable = false)
  private int totalVisits;

  @Column(name = "authenticated_visits", nullable = false)
  private int authenticatedVisits;

  @Builder
  private DailyVisit(
      LocalDate visitDate, int uniqueVisitors, int totalVisits, int authenticatedVisits) {
    this.visitDate = visitDate;
    this.uniqueVisitors = uniqueVisitors;
    this.totalVisits = totalVisits;
    this.authenticatedVisits = authenticatedVisits;
  }

  /** UPSERT 시 기존 row 를 덮어쓸 때 사용 (같은 날짜 재flush 대응). */
  public void overwrite(int uniqueVisitors, int totalVisits, int authenticatedVisits) {
    this.uniqueVisitors = uniqueVisitors;
    this.totalVisits = totalVisits;
    this.authenticatedVisits = authenticatedVisits;
  }
}
