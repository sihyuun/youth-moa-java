package io.github.sihyuuun.youthmoa.common.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.program.CalendarViewDto;
import io.github.sihyuuun.youthmoa.program.ProgramCardDto;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D-admin-calendar (2026-10-12): CalendarGridBuilder 공용 42셀 유틸 단위 테스트.
 *
 * <p>POLICY P-6 "월간 캘린더 42셀 6행 고정" 를 기계적으로 검증한다. ProgramCalendarService / AdminProgramCalendarService
 * 양쪽이 공유하는 지점.
 */
class CalendarGridBuilderTest {

  @Test
  @DisplayName("build42Cells 는 어느 월이든 항상 42개 셀 (POLICY P-6)")
  void cells_size_always_42() {
    for (int[] ym : new int[][] {{2026, 1}, {2026, 2}, {2026, 8}, {2026, 12}, {2024, 2}}) {
      List<CalendarViewDto.CalendarCell> cells =
          CalendarGridBuilder.build42Cells(
              YearMonth.of(ym[0], ym[1]), LocalDate.of(2026, 1, 1), Map.of(), c -> "open", 2);
      assertThat(cells).as("ym %d-%d", ym[0], ym[1]).hasSize(42);
    }
  }

  @Test
  @DisplayName("2026-08-01 은 토요일 → 앞 빈 셀 6개, cells[6] = 1일")
  void first_dow_of_2026_08_is_saturday_6() {
    List<CalendarViewDto.CalendarCell> cells =
        CalendarGridBuilder.build42Cells(
            YearMonth.of(2026, 8), LocalDate.of(2026, 8, 1), Map.of(), c -> "open", 2);
    for (int i = 0; i < 6; i++) {
      assertThat(cells.get(i).getDay()).as("cell %d", i).isNull();
      assertThat(cells.get(i).isInMonth()).isFalse();
    }
    assertThat(cells.get(6).getDay()).isEqualTo(1);
    assertThat(cells.get(6).isInMonth()).isTrue();
  }

  @Test
  @DisplayName("2026-08: cells[36]=31일, cells[37]부터 null")
  void days_placement_and_trailing_blanks() {
    List<CalendarViewDto.CalendarCell> cells =
        CalendarGridBuilder.build42Cells(
            YearMonth.of(2026, 8), LocalDate.of(2026, 8, 1), Map.of(), c -> "open", 2);
    assertThat(cells.get(36).getDay()).isEqualTo(31);
    assertThat(cells.get(37).getDay()).isNull();
    assertThat(cells.get(41).getDay()).isNull();
  }

  @Test
  @DisplayName("today 플래그는 cellDate == today 일 때만 true")
  void today_flag_matches_cell_date() {
    LocalDate today = LocalDate.of(2026, 8, 15);
    List<CalendarViewDto.CalendarCell> cells =
        CalendarGridBuilder.build42Cells(
            YearMonth.of(2026, 8), today, Map.of(), c -> "open", 2);
    // 15일 셀의 today 는 true, 나머지 inMonth 셀은 false
    long trueCount = cells.stream().filter(CalendarViewDto.CalendarCell::isToday).count();
    assertThat(trueCount).isEqualTo(1L);
    assertThat(cells.get(6 + 14).getDay()).isEqualTo(15);
    assertThat(cells.get(6 + 14).isToday()).isTrue();
  }

  @Test
  @DisplayName("prevMonth/nextMonth 는 YearMonth 경계를 넘긴다")
  void prev_next_month_crosses_year_boundary() {
    YearMonth jan = YearMonth.of(2026, 1);
    assertThat(CalendarGridBuilder.prevMonth(jan)).isEqualTo(YearMonth.of(2025, 12));
    YearMonth dec = YearMonth.of(2026, 12);
    assertThat(CalendarGridBuilder.nextMonth(dec)).isEqualTo(YearMonth.of(2027, 1));
  }

  @Test
  @DisplayName("nearestMonth: candidate empty → Optional.empty")
  void nearest_month_empty_candidates() {
    assertThat(CalendarGridBuilder.nearestMonth(List.of(), YearMonth.of(2026, 8))).isEmpty();
    assertThat(CalendarGridBuilder.nearestMonth(null, YearMonth.of(2026, 8))).isEmpty();
  }

  @Test
  @DisplayName("nearestMonth: pivot 과 같은 월만 있으면 Optional.empty")
  void nearest_month_only_same_month() {
    Optional<YearMonth> result =
        CalendarGridBuilder.nearestMonth(
            List.of(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 15)), YearMonth.of(2026, 8));
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("nearestMonth: 동거리 tie-break 는 미래 우선 (pivot 8월, 6월·10월 → 10월)")
  void nearest_month_tie_break_prefers_future() {
    Optional<YearMonth> result =
        CalendarGridBuilder.nearestMonth(
            List.of(LocalDate.of(2026, 6, 15), LocalDate.of(2026, 10, 15)),
            YearMonth.of(2026, 8));
    assertThat(result).contains(YearMonth.of(2026, 10));
  }

  @Test
  @DisplayName("nearestMonth: 거리 다르면 가까운 쪽 선택 (pivot 8월, 7월·12월 → 7월)")
  void nearest_month_closer_wins() {
    Optional<YearMonth> result =
        CalendarGridBuilder.nearestMonth(
            List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 1)), YearMonth.of(2026, 8));
    assertThat(result).contains(YearMonth.of(2026, 7));
  }

  @Test
  @DisplayName("maxPills 초과 시 moreCount 로 집계")
  void more_count_for_excess_pills() {
    ProgramCardDto a = stubCard(1L, "A");
    ProgramCardDto b = stubCard(2L, "B");
    ProgramCardDto c = stubCard(3L, "C");
    List<CalendarViewDto.CalendarCell> cells =
        CalendarGridBuilder.build42Cells(
            YearMonth.of(2026, 8),
            LocalDate.of(2026, 8, 1),
            Map.of(15, List.of(a, b, c)),
            card -> "open",
            2);
    CalendarViewDto.CalendarCell cell = cells.get(6 + 14);
    assertThat(cell.getDay()).isEqualTo(15);
    assertThat(cell.getPills()).hasSize(2);
    assertThat(cell.getMoreCount()).isEqualTo(1);
  }

  private static ProgramCardDto stubCard(long id, String title) {
    // ProgramCardDto 는 Program 엔티티를 요구. 단위 테스트 범위에서는 build42Cells 가
    // id/title/pillColor 함수 결과만 사용하므로 최소 엔티티로 충분.
    io.github.sihyuuun.youthmoa.program.Program p =
        io.github.sihyuuun.youthmoa.program.Program.builder()
            .title(title)
            .category("c")
            .region("r")
            .content("c")
            .startDate(LocalDate.of(2026, 8, 15))
            .endDate(LocalDate.of(2099, 12, 31))
            .capacity(10)
            .build();
    try {
      java.lang.reflect.Field f =
          io.github.sihyuuun.youthmoa.program.Program.class.getSuperclass().getDeclaredField("id");
      f.setAccessible(true);
      f.set(p, id);
    } catch (ReflectiveOperationException ignored) {
      // Program.id 가 BaseTimeEntity 상속이 아니면 조용히 무시. pill id 검증은 다른 TC 에서.
      try {
        java.lang.reflect.Field f =
            io.github.sihyuuun.youthmoa.program.Program.class.getDeclaredField("id");
        f.setAccessible(true);
        f.set(p, id);
      } catch (ReflectiveOperationException e) {
        // ignore
      }
    }
    return new ProgramCardDto(p, 0);
  }
}
