package io.github.sihyuuun.youthmoa.program;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * 프로그램 캘린더 뷰 (`?view=calendar`) 서버 모델. dc.html §1a / §5a / §7a 정본 반영.
 *
 * <p>구성:
 *
 * <ul>
 *   <li>{@link #cells} — 7×6 = 42 셀 고정 (dc.html §1a, Q1 결정)
 *   <li>{@link #panelData} — 우측 패널용 pre-render 데이터 (day-of-month → 프로그램 카드 리스트)
 *   <li>{@link #nearestMonth} — 빈 달 배너용, 필터 결과가 있는 가장 가까운 달. 없으면 null
 * </ul>
 */
@Getter
@Builder
public class CalendarViewDto {

  private final int year;
  private final int month;
  private final LocalDate today;
  private final List<CalendarCell> cells;
  private final int totalCount; // 이번 달 프로그램 카운트 (배너 판단용)

  // 월 이동 링크 파라미터
  private final int prevYear;
  private final int prevMonth;
  private final int nextYear;
  private final int nextMonth;

  // 빈 달 배너
  private final Integer nearestYear;
  private final Integer nearestMonth;
  private final long nearestCount;

  // 우측 패널: dayOfMonth → 카드 리스트
  private final Map<Integer, List<ProgramCardDto>> panelData;

  @Getter
  @Builder
  public static class CalendarCell {
    /** 셀에 표시할 일. 다른 달 (앞·뒤) 공백 셀이면 null */
    private final Integer day;
    /** 요일 (0=일, 6=토) */
    private final int dow;
    /** 이번 달 셀 여부 */
    private final boolean inMonth;
    /** 오늘 셀 여부 */
    private final boolean today;
    /** 최대 2건 pill */
    private final List<ProgramPill> pills;
    /** 초과 개수 (pills 2건 이후) */
    private final int moreCount;
  }

  @Getter
  @Builder
  public static class ProgramPill {
    private final Long id;
    private final String title;
    /** upcoming | open | ended (dc.html §5a 확정 3색) */
    private final String colorKind;
  }
}
