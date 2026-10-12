package io.github.sihyuuun.youthmoa.common.calendar;

import io.github.sihyuuun.youthmoa.program.CalendarViewDto;
import io.github.sihyuuun.youthmoa.program.ProgramCardDto;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * D-admin-calendar (2026-10-12): 월간 캘린더 공용 42셀 산출 유틸.
 *
 * <p>POLICY P-6 "월간 캘린더 42셀 6행 고정" 를 기계적으로 보장한다. 사용자({@code ProgramCalendarService}) 와 관리자({@code
 * AdminProgramCalendarService}) 양쪽에서 재사용한다.
 *
 * <p>책임 분리:
 *
 * <ul>
 *   <li>본 유틸: 42셀 좌표 산출, prev/next 월 계산, nearestMonth tie-break
 *   <li>호출자: Program 조회·필터·pill 색 매핑·pill 라벨 결정
 * </ul>
 */
public final class CalendarGridBuilder {

  public static final int CELL_COUNT = 42;
  public static final int ROWS = 6;
  public static final int COLS = 7;

  private CalendarGridBuilder() {}

  /**
   * 42셀 산출.
   *
   * @param ym 표시 대상 월
   * @param today 오늘 날짜 (today 플래그 계산용)
   * @param byDay dayOfMonth → 해당 날짜의 ProgramCardDto 리스트 (startDate 기준 그룹)
   * @param pillColor ProgramCardDto → pill colorKind 변환 함수 (upcoming/open/ended)
   * @param maxPills 셀당 최대 pill 수 (초과분은 moreCount)
   */
  public static List<CalendarViewDto.CalendarCell> build42Cells(
      YearMonth ym,
      LocalDate today,
      Map<Integer, List<ProgramCardDto>> byDay,
      Function<ProgramCardDto, String> pillColor,
      int maxPills) {
    LocalDate firstOfMonth = ym.atDay(1);
    // Java DayOfWeek: 월=1..일=7. 캘린더는 일=0 기준 → firstDow = getDayOfWeek().getValue() % 7
    int firstDow = firstOfMonth.getDayOfWeek().getValue() % 7;
    int daysInMonth = ym.lengthOfMonth();

    List<CalendarViewDto.CalendarCell> cells = new ArrayList<>(CELL_COUNT);
    for (int i = 0; i < CELL_COUNT; i++) {
      int dayIdx = i - firstDow + 1;
      int dow = i % COLS;
      if (dayIdx < 1 || dayIdx > daysInMonth) {
        cells.add(
            CalendarViewDto.CalendarCell.builder()
                .day(null)
                .dow(dow)
                .inMonth(false)
                .today(false)
                .pills(List.of())
                .moreCount(0)
                .build());
      } else {
        List<ProgramCardDto> dayCards = byDay.getOrDefault(dayIdx, List.of());
        List<CalendarViewDto.ProgramPill> pills = new ArrayList<>();
        int limit = Math.min(maxPills, dayCards.size());
        for (int j = 0; j < limit; j++) {
          ProgramCardDto c = dayCards.get(j);
          pills.add(
              CalendarViewDto.ProgramPill.builder()
                  .id(c.getId())
                  .title(c.getTitle())
                  .colorKind(pillColor.apply(c))
                  .build());
        }
        int more = Math.max(0, dayCards.size() - maxPills);
        LocalDate cellDate = ym.atDay(dayIdx);
        cells.add(
            CalendarViewDto.CalendarCell.builder()
                .day(dayIdx)
                .dow(dow)
                .inMonth(true)
                .today(cellDate.equals(today))
                .pills(pills)
                .moreCount(more)
                .build());
      }
    }
    return cells;
  }

  /** 이전·다음 월 (YearMonth 쌍). */
  public static YearMonth prevMonth(YearMonth ym) {
    return ym.minusMonths(1);
  }

  public static YearMonth nextMonth(YearMonth ym) {
    return ym.plusMonths(1);
  }

  /**
   * 빈 달 배너용 nearestMonth 산출. pivot 월과 다른 월 중 **월 거리 최소**, tie-break 는 **미래 우선**.
   *
   * @param candidateDates pivot 과 동일 월도 섞여 있을 수 있는 LocalDate 리스트. null/empty 는 Optional.empty.
   */
  public static Optional<YearMonth> nearestMonth(List<LocalDate> candidateDates, YearMonth pivot) {
    if (candidateDates == null || candidateDates.isEmpty()) return Optional.empty();
    return candidateDates.stream()
        .filter(d -> d != null)
        .map(YearMonth::from)
        .filter(pym -> !pym.equals(pivot))
        .min(
            Comparator.<YearMonth>comparingLong(pym -> Math.abs(monthsBetween(pym, pivot)))
                .thenComparingInt(pym -> monthsBetween(pym, pivot) >= 0 ? 0 : 1));
  }

  public static long monthsBetween(YearMonth a, YearMonth b) {
    return (a.getYear() - b.getYear()) * 12L + (a.getMonthValue() - b.getMonthValue());
  }
}
