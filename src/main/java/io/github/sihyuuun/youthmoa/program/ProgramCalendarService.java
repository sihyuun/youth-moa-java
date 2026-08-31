package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프로그램 캘린더 뷰 (`?view=calendar`) 서버 렌더 서비스. dc.html §1a/§5a/§6a/§7a 정본 반영.
 *
 * <p>정책:
 *
 * <ul>
 *   <li>grid = 7×6 = 42 셀 고정 (Q1)
 *   <li>grouping key = startDate.dayOfMonth (Q2, §6a)
 *   <li>셀 pill 색 = 3색 매핑 (upcoming / open / ended, §5a)
 *   <li>pill 최대 2건, 초과는 "+N건 더" (Q6)
 *   <li>여러 날 걸침 프로그램도 startDate 셀에만 pill (Q8)
 *   <li>빈 달일 때 nearestMonth 조회 (§7a)
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramCalendarService {

  private static final List<ApplicationStatus> ACTIVE_STATUSES =
      List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED);
  private static final int MAX_PILLS = 2;

  private final ProgramRepository programRepository;
  private final ApplicationRepository applicationRepository;

  /**
   * 캘린더 뷰 렌더 데이터 산출.
   *
   * @param status 필터 상태 (open/upcoming/ended/"")
   * @param regions 지역 필터
   * @param centers 청년센터 필터
   * @param year 표시 연도
   * @param month 표시 월 (1~12)
   */
  public CalendarViewDto calendar(
      String status, List<String> regions, List<String> centers, int year, int month) {

    YearMonth ym = YearMonth.of(year, month);
    LocalDate today = LocalDate.now();
    LocalDate firstOfMonth = ym.atDay(1);
    LocalDate lastOfMonth = ym.atEndOfMonth();

    // 목록 뷰와 동일 필터 파이프라인 재사용 (calendarSource = filtered, dc.html §10)
    Specification<Program> spec = Specification.where(ProgramSpec.isActive());
    Specification<Program> dateSpec = ProgramSpec.withDateStatus(status);
    if (dateSpec != null) {
      spec = spec.and(dateSpec);
    } else {
      // "전체" 탭도 종료는 제외 (list 와 동일 정책)
      spec = spec.and(ProgramSpec.notEnded());
    }
    Specification<Program> regionSpec = ProgramSpec.withRegions(regions);
    if (regionSpec != null) spec = spec.and(regionSpec);
    Specification<Program> centerSpec = ProgramSpec.withCenters(centers);
    if (centerSpec != null) spec = spec.and(centerSpec);

    // 이번 달 프로그램: startDate ∈ [firstOfMonth, lastOfMonth]
    Specification<Program> monthSpec =
        spec.and(
            (root, q, cb) ->
                cb.and(
                    cb.isNotNull(root.get("startDate")),
                    cb.greaterThanOrEqualTo(root.get("startDate"), firstOfMonth),
                    cb.lessThanOrEqualTo(root.get("startDate"), lastOfMonth)));

    List<Program> monthPrograms =
        programRepository.findAll(monthSpec, Sort.by(Sort.Direction.ASC, "startDate", "id"));

    // ProgramCardDto 변환 (신청자수 IN 쿼리 1회)
    Map<Long, Long> appliedMap = new java.util.HashMap<>();
    if (!monthPrograms.isEmpty()) {
      List<Long> ids = monthPrograms.stream().map(Program::getId).toList();
      applicationRepository
          .countByProgramIdsAndStatuses(ids, ACTIVE_STATUSES)
          .forEach(row -> appliedMap.put((Long) row[0], (Long) row[1]));
    }

    // dayOfMonth → 카드 그룹
    Map<Integer, List<ProgramCardDto>> byDay = new TreeMap<>();
    for (Program p : monthPrograms) {
      int day = p.getStartDate().getDayOfMonth();
      ProgramCardDto card = new ProgramCardDto(p, appliedMap.getOrDefault(p.getId(), 0L));
      byDay.computeIfAbsent(day, k -> new ArrayList<>()).add(card);
    }

    // 42셀 산출
    // Java DayOfWeek: 월=1..일=7. dc.html 은 일=0 기준 → firstDow = firstOfMonth.getDayOfWeek() % 7
    int firstDow = firstOfMonth.getDayOfWeek().getValue() % 7;
    int daysInMonth = ym.lengthOfMonth();

    List<CalendarViewDto.CalendarCell> cells = new ArrayList<>(42);
    for (int i = 0; i < 42; i++) {
      int dayIdx = i - firstDow + 1;
      int dow = i % 7;
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
        int limit = Math.min(MAX_PILLS, dayCards.size());
        for (int j = 0; j < limit; j++) {
          ProgramCardDto c = dayCards.get(j);
          pills.add(
              CalendarViewDto.ProgramPill.builder()
                  .id(c.getId())
                  .title(c.getTitle())
                  .colorKind(pillColorKind(c))
                  .build());
        }
        int more = Math.max(0, dayCards.size() - MAX_PILLS);
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

    // panelData — dayOfMonth → 카드 리스트 (LinkedHashMap 순서 유지)
    Map<Integer, List<ProgramCardDto>> panelData = new LinkedHashMap<>(byDay);

    // 빈 달 배너용 nearestMonth 조회
    Integer nearestYear = null;
    Integer nearestMonth = null;
    long nearestCount = 0;
    if (monthPrograms.isEmpty()) {
      // 필터 결과 전체에서 startDate 가 이번 달과 다른 프로그램들의 startDate max 를 찾음
      // 우선 이번 달 이전에서 max, 없으면 이후에서 min
      List<Program> otherPrograms =
          programRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "startDate"));
      if (!otherPrograms.isEmpty()) {
        // 이번 달과 가장 가까운 (startDate 절대 거리) 프로그램의 월을 선택
        // 동거리(예: 6월/10월 각 1건 in 8월 기준) tie-break: 미래(>=pivot) 우선 (스펙 §3-A #9)
        YearMonth pivot = ym;
        Program nearest =
            otherPrograms.stream()
                .filter(p -> p.getStartDate() != null)
                .filter(
                    p -> {
                      YearMonth pym = YearMonth.from(p.getStartDate());
                      return !pym.equals(pivot);
                    })
                .min(
                    Comparator.<Program>comparingLong(
                            p -> Math.abs(monthsBetween(YearMonth.from(p.getStartDate()), pivot)))
                        .thenComparingInt(
                            p ->
                                monthsBetween(YearMonth.from(p.getStartDate()), pivot) >= 0
                                    ? 0
                                    : 1))
                .orElse(null);
        if (nearest != null) {
          YearMonth nym = YearMonth.from(nearest.getStartDate());
          nearestYear = nym.getYear();
          nearestMonth = nym.getMonthValue();
          // 해당 월 카운트
          YearMonth n = nym;
          Specification<Program> countSpec =
              spec.and(
                  (root, q, cb) ->
                      cb.and(
                          cb.isNotNull(root.get("startDate")),
                          cb.greaterThanOrEqualTo(root.get("startDate"), n.atDay(1)),
                          cb.lessThanOrEqualTo(root.get("startDate"), n.atEndOfMonth())));
          nearestCount = programRepository.count(countSpec);
        }
      }
    }

    YearMonth prev = ym.minusMonths(1);
    YearMonth next = ym.plusMonths(1);

    return CalendarViewDto.builder()
        .year(year)
        .month(month)
        .today(today)
        .cells(cells)
        .totalCount(monthPrograms.size())
        .prevYear(prev.getYear())
        .prevMonth(prev.getMonthValue())
        .nextYear(next.getYear())
        .nextMonth(next.getMonthValue())
        .nearestYear(nearestYear)
        .nearestMonth(nearestMonth)
        .nearestCount(nearestCount)
        .panelData(panelData)
        .build();
  }

  private static long monthsBetween(YearMonth a, YearMonth b) {
    return (a.getYear() - b.getYear()) * 12L + (a.getMonthValue() - b.getMonthValue());
  }

  /** dc.html §5a 3색 매핑. isFull 은 종료 회색 편입. */
  static String pillColorKind(ProgramCardDto card) {
    ProgramStatus status = card.getStatus();
    if (status == ProgramStatus.UPCOMING) return "upcoming";
    if (status == ProgramStatus.ENDED) return "ended";
    // OPEN
    Integer capacity = card.getCapacity();
    if (capacity != null && capacity > 0 && card.getApplicantCount() >= capacity) {
      return "ended"; // isFull → 종료 회색 편입
    }
    return "open";
  }
}
