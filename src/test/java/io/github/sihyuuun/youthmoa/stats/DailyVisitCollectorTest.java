package io.github.sihyuuun.youthmoa.stats;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DailyVisitCollectorTest {

  private final DailyVisitCollector collector = new DailyVisitCollector();

  @Test
  void record_incrementsTotalVisits() {
    LocalDate d = LocalDate.of(2026, 9, 16);
    collector.record(d, "s1", false);
    collector.record(d, "s1", false);
    collector.record(d, "s2", true);

    DailyVisitCollector.VisitCounts snap = collector.snapshot().get(d);
    assertThat(snap.totalVisits()).isEqualTo(3);
    assertThat(snap.uniqueVisitors()).isEqualTo(2); // s1, s2
    assertThat(snap.authenticatedVisits()).isEqualTo(1);
  }

  @Test
  void drain_removesEntry() {
    LocalDate d = LocalDate.of(2026, 9, 15);
    collector.record(d, "x", false);
    DailyVisitCollector.VisitCounts drained = collector.drain(d);
    assertThat(drained.totalVisits()).isEqualTo(1);
    assertThat(collector.snapshot()).doesNotContainKey(d);
  }

  @Test
  void drain_missingDate_returnsZero() {
    DailyVisitCollector.VisitCounts drained = collector.drain(LocalDate.of(1999, 1, 1));
    assertThat(drained.totalVisits()).isEqualTo(0);
    assertThat(drained.uniqueVisitors()).isEqualTo(0);
  }

  @Test
  void record_nullDate_noop() {
    collector.record(null, "s", false);
    assertThat(collector.snapshot()).isEmpty();
  }
}
