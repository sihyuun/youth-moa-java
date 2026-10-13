package io.github.sihyuuun.youthmoa.admin.program;

import io.github.sihyuuun.youthmoa.admin.AdminProgramService;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.common.calendar.CalendarGridBuilder;
import io.github.sihyuuun.youthmoa.program.CalendarViewDto;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramCalendarService;
import io.github.sihyuuun.youthmoa.program.ProgramCardDto;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.ProgramStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * D-admin-calendar (2026-10-12): 관리자 프로그램 캘린더 뷰(`/admin/programs?view=calendar`) 서버 렌더 서비스.
 *
 * <p>사용자 {@code ProgramCalendarService} 와 동일한 42셀 산출(CalendarGridBuilder) 을 재사용하되, 다음 차이가 있다:
 *
 * <ul>
 *   <li><b>SUSPENDED 포함</b> (Q6) — 관리자는 운영 중단된 프로그램도 보여야 하므로 {@code isActive=true} 필터를 걸지 않는다
 *   <li><b>URL filter pass-through</b> (Q7) — 목록 뷰의 q·status 필터를 {@link
 *       AdminProgramService#buildListSpec} 로 재사용
 *   <li><b>read-only</b> (Q5) — 셀/pill 클릭 시 상세 이동만. 캘린더 내 CRUD 없음
 * </ul>
 *
 * <p>POLICY P-6 "월간 캘린더 42셀 6행 고정" 적용.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProgramCalendarService {

  private static final List<ApplicationStatus> ACTIVE_STATUSES =
      List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED);
  private static final int MAX_PILLS = 2;

  private final ProgramRepository programRepository;
  private final ApplicationRepository applicationRepository;
  private final AdminProgramService adminProgramService;

  /**
   * 캘린더 뷰 렌더 데이터 산출.
   *
   * @param q 검색어 (title + center.name LIKE)
   * @param status 상태 필터 (OPEN|UPCOMING|ENDED|SUSPENDED|null)
   * @param year 표시 연도
   * @param month 표시 월 (1~12)
   */
  public CalendarViewDto calendar(String q, String status, int year, int month) {
    YearMonth ym = YearMonth.of(year, month);
    LocalDate today = LocalDate.now();
    LocalDate firstOfMonth = ym.atDay(1);
    LocalDate lastOfMonth = ym.atEndOfMonth();

    // 목록 뷰와 동일 필터 파이프라인 재사용 (Q7)
    Specification<Program> spec = adminProgramService.buildListSpec(q, status);

    // 이번 달 프로그램: startDate ∈ [firstOfMonth, lastOfMonth]
    Specification<Program> monthSpec =
        spec.and(
            (root, qq, cb) ->
                cb.and(
                    cb.isNotNull(root.get("startDate")),
                    cb.greaterThanOrEqualTo(root.get("startDate"), firstOfMonth),
                    cb.lessThanOrEqualTo(root.get("startDate"), lastOfMonth)));

    List<Program> monthPrograms =
        programRepository.findAll(monthSpec, Sort.by(Sort.Direction.ASC, "startDate", "id"));

    // ProgramCardDto 변환 (신청자수 IN 쿼리 1회)
    Map<Long, Long> appliedMap = new HashMap<>();
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

    // 42셀 산출 — CalendarGridBuilder 공용 유틸 (POLICY P-6)
    List<CalendarViewDto.CalendarCell> cells =
        CalendarGridBuilder.build42Cells(
            ym, today, byDay, AdminProgramCalendarService::pillColorKind, MAX_PILLS);

    // panelData — dayOfMonth → 카드 리스트 (LinkedHashMap 순서 유지)
    Map<Integer, List<ProgramCardDto>> panelData = new LinkedHashMap<>(byDay);

    // 빈 달 배너용 nearestMonth 조회
    Integer nearestYear = null;
    Integer nearestMonth = null;
    long nearestCount = 0;
    if (monthPrograms.isEmpty()) {
      List<Program> otherPrograms =
          programRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "startDate"));
      if (!otherPrograms.isEmpty()) {
        List<LocalDate> candidateDates =
            otherPrograms.stream().map(Program::getStartDate).filter(d -> d != null).toList();
        Optional<YearMonth> nym = CalendarGridBuilder.nearestMonth(candidateDates, ym);
        if (nym.isPresent()) {
          YearMonth n = nym.get();
          nearestYear = n.getYear();
          nearestMonth = n.getMonthValue();
          Specification<Program> countSpec =
              spec.and(
                  (root, qq, cb) ->
                      cb.and(
                          cb.isNotNull(root.get("startDate")),
                          cb.greaterThanOrEqualTo(root.get("startDate"), n.atDay(1)),
                          cb.lessThanOrEqualTo(root.get("startDate"), n.atEndOfMonth())));
          nearestCount = programRepository.count(countSpec);
        }
      }
    }

    YearMonth prev = CalendarGridBuilder.prevMonth(ym);
    YearMonth next = CalendarGridBuilder.nextMonth(ym);

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

  /**
   * admin 전용 pill 색 매핑. 사용자 뷰({@link ProgramCalendarService#pillColorKind})와 거의 동일하되, SUSPENDED 는
   * ended(회색)로 매핑해 운영 중단 프로그램을 시각적으로 구분한다.
   */
  static String pillColorKind(ProgramCardDto card) {
    ProgramStatus status = card.getStatus();
    if (status == ProgramStatus.SUSPENDED) return "ended";
    if (status == ProgramStatus.UPCOMING) return "upcoming";
    if (status == ProgramStatus.ENDED) return "ended";
    Integer capacity = card.getCapacity();
    if (capacity != null && capacity > 0 && card.getApplicantCount() >= capacity) {
      return "ended";
    }
    return "open";
  }
}
