package io.github.sihyuuun.youthmoa.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * F0h-operating-hours-badge: CSV 15컬럼 파싱 회귀 방어.
 *
 * <p>파싱 불가 3행 (28청춘창업소·양주시청년센터·의정부 청년다락방) 은 weekdayOpen/Close 가 빈값 → schedule=null 반환. 나머지 행은 요일별
 * open/close 채워진 {@link io.github.sihyuuun.youthmoa.center.OperatingHours} 를 갖는다.
 */
class CenterCsvLoaderTest {

  private final CenterCsvLoader loader = new CenterCsvLoader();

  private Optional<CenterCsvRow> findByName(List<CenterCsvRow> rows, String name) {
    return rows.stream().filter(r -> name.equals(r.name())).findFirst();
  }

  @Test
  void 시드_48행_로드() {
    List<CenterCsvRow> rows = loader.load();
    assertThat(rows).hasSize(48);
  }

  @Test
  void 정상행_schedule_파싱_확인_내일꿈제작소() {
    List<CenterCsvRow> rows = loader.load();
    CenterCsvRow row = findByName(rows, "내일꿈제작소").orElseThrow();
    assertThat(row.schedule()).isNotNull();
    assertThat(row.schedule().getWeekdayOpen()).isEqualTo(LocalTime.of(10, 0));
    assertThat(row.schedule().getWeekdayClose()).isEqualTo(LocalTime.of(18, 0));
    assertThat(row.schedule().getSaturdayOpen()).isEqualTo(LocalTime.of(10, 0));
    assertThat(row.schedule().getSaturdayClose()).isEqualTo(LocalTime.of(18, 0));
    assertThat(row.schedule().getSundayOpen()).isNull();
    assertThat(row.schedule().getSundayClose()).isNull();
    assertThat(row.schedule().isHolidayClosed()).isTrue();
  }

  @Test
  void 파싱불가_행_28청춘창업소_schedule_null() {
    List<CenterCsvRow> rows = loader.load();
    CenterCsvRow row = findByName(rows, "28청춘창업소").orElseThrow();
    assertThat(row.schedule()).isNull();
  }

  @Test
  void 파싱불가_행_양주시청년센터_schedule_null() {
    List<CenterCsvRow> rows = loader.load();
    CenterCsvRow row = findByName(rows, "양주시청년센터").orElseThrow();
    assertThat(row.schedule()).isNull();
  }

  @Test
  void 파싱불가_행_의정부_청년다락방_schedule_null() {
    List<CenterCsvRow> rows = loader.load();
    CenterCsvRow row = findByName(rows, "의정부시 청년다락방").orElseThrow();
    assertThat(row.schedule()).isNull();
  }

  @Test
  void 일요일_운영_센터_schedule_sundayOpen_반영_청춘곳간() {
    List<CenterCsvRow> rows = loader.load();
    CenterCsvRow row = findByName(rows, "청춘곳간").orElseThrow();
    assertThat(row.schedule()).isNotNull();
    assertThat(row.schedule().getSundayOpen()).isEqualTo(LocalTime.of(9, 0));
    assertThat(row.schedule().getSundayClose()).isEqualTo(LocalTime.of(18, 0));
  }

  @Test
  void 정상행_모두_schedule_존재_45개() {
    List<CenterCsvRow> rows = loader.load();
    long withSchedule = rows.stream().filter(r -> r.schedule() != null).count();
    // 48 - 3(파싱 불가) = 45
    assertThat(withSchedule).isEqualTo(45L);
  }
}
