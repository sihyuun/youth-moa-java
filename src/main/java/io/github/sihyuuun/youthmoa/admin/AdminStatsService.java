package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.admin.chart.SvgChartRenderer;
import io.github.sihyuuun.youthmoa.admin.chart.SvgChartRenderer.DonutSlice;
import io.github.sihyuuun.youthmoa.admin.chart.SvgChartRenderer.SeriesData;
import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
import io.github.sihyuuun.youthmoa.program.ProgramStatus;
import io.github.sihyuuun.youthmoa.stats.DailyVisit;
import io.github.sihyuuun.youthmoa.stats.DailyVisitRepository;
import io.github.sihyuuun.youthmoa.user.AgeBucket;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserGender;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A6 admin-stats (2026-09-16) — 관리자 통계 대시보드 데이터 소스.
 *
 * <p>KPI · 방문자 차트 · 성별/연령 도넛 · 프로그램별 참여 · 마감임박 · 승인대기.
 *
 * <ul>
 *   <li>성별/연령 = User.gender + User.birthDate 실시간 GROUP BY (Qn-A/B · 캐시 없음)
 *   <li>방문자 = DailyVisit 최근 30일 (없는 날은 0 표시 · Qn-8)
 *   <li>KPI 증감 = 최근 30일 vs 이전 30일 방문자 SUM 비교 (Qn-9)
 *   <li>마감임박 = applyEndDate 우선 (없으면 endDate) · 오늘~+7 (Qn-10)
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AdminStatsService {

  private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM.dd");
  private static final DateTimeFormatter DATE_LABEL = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final ProgramRepository programRepository;
  private final ApplicationRepository applicationRepository;
  private final UserRepository userRepository;
  private final DailyVisitRepository dailyVisitRepository;
  private final SvgChartRenderer chartRenderer;

  @Transactional(readOnly = true)
  public StatsModel load(Long scopeCenterId, String chartMode) {
    LocalDate today = LocalDate.now();
    boolean yearMode = "year".equalsIgnoreCase(chartMode);

    // ── scoped 프로그램·사용자 ────────────────────────────────
    List<Program> scoped = scopedPrograms(scopeCenterId);
    List<User> scopedUsers = scopedUsers(scopeCenterId);

    // ── KPI ──────────────────────────────────────────────────
    long totalPrograms = scoped.size();
    long totalUsers = scopedUsers.stream().filter(u -> u.getRole() == UserRole.USER).count();
    long pending = countPendingApplications(scoped);

    // 최근 30일 vs 이전 30일 방문자 SUM (Qn-9)
    LocalDate cur30From = today.minusDays(29);
    LocalDate prev30From = today.minusDays(59);
    LocalDate prev30To = today.minusDays(30);
    long visitors30 = sumVisitors(cur30From, today);
    long visitorsPrev30 = sumVisitors(prev30From, prev30To);
    int visitorsDeltaPct = pctDelta(visitors30, visitorsPrev30);

    // ── 방문자 차트 ────────────────────────────────────────────
    SeriesData visitorSeries = yearMode ? yearlySeries(today) : dailySeries(today, 30);
    String chartSvg =
        yearMode
            ? chartRenderer.createLineChart(visitorSeries)
            : chartRenderer.createBarChart(visitorSeries);

    long totalVisits = visitorSeries.values().stream().mapToLong(Long::longValue).sum();
    long avgVisits =
        visitorSeries.values().isEmpty() ? 0 : totalVisits / visitorSeries.values().size();
    long peakVisits = visitorSeries.values().stream().mapToLong(Long::longValue).max().orElse(0);

    // ── 성별 분포 (Qn-A) ───────────────────────────────────────
    Map<UserGender, Long> genderCount = new EnumMap<>(UserGender.class);
    long genderNull = 0;
    for (User u : scopedUsers) {
      if (u.getRole() != UserRole.USER) continue;
      if (u.getGender() == null) genderNull++;
      else genderCount.merge(u.getGender(), 1L, Long::sum);
    }
    List<DonutSlice> genderData = new ArrayList<>();
    long maleN = genderCount.getOrDefault(UserGender.MALE, 0L);
    long femaleN = genderCount.getOrDefault(UserGender.FEMALE, 0L);
    if (maleN > 0) genderData.add(new DonutSlice("남성", maleN, SvgChartRenderer.COLOR_PRIMARY));
    if (femaleN > 0)
      genderData.add(new DonutSlice("여성", femaleN, SvgChartRenderer.COLOR_SECONDARY));
    if (genderNull > 0)
      genderData.add(new DonutSlice("무응답", genderNull, SvgChartRenderer.COLOR_MUTED));
    String genderChartSvg = chartRenderer.createDonutChart(genderData);
    long genderTotal = genderData.stream().mapToLong(DonutSlice::value).sum();
    List<LegendEntry> genderLegend = toLegend(genderData, genderTotal);

    // ── 연령대 분포 (Qn-B) ─────────────────────────────────────
    Map<AgeBucket, Long> ageCount = new EnumMap<>(AgeBucket.class);
    for (User u : scopedUsers) {
      if (u.getRole() != UserRole.USER) continue;
      AgeBucket b = AgeBucket.of(u.getBirthDate(), today);
      ageCount.merge(b, 1L, Long::sum);
    }
    List<DonutSlice> ageData = new ArrayList<>();
    List<String> palette = SvgChartRenderer.DONUT_PALETTE;
    int paletteIdx = 0;
    for (AgeBucket b :
        new AgeBucket[] {
          AgeBucket.AGE_19_24,
          AgeBucket.AGE_25_29,
          AgeBucket.AGE_30_34,
          AgeBucket.AGE_35_39,
          AgeBucket.AGE_40_PLUS
        }) {
      long n = ageCount.getOrDefault(b, 0L);
      if (n > 0)
        ageData.add(new DonutSlice(b.getLabel(), n, palette.get(paletteIdx % palette.size())));
      paletteIdx++;
    }
    long unknownN = ageCount.getOrDefault(AgeBucket.UNKNOWN, 0L);
    if (unknownN > 0)
      ageData.add(
          new DonutSlice(AgeBucket.UNKNOWN.getLabel(), unknownN, SvgChartRenderer.COLOR_MUTED));
    String ageChartSvg = chartRenderer.createDonutChart(ageData);
    long ageTotal = ageData.stream().mapToLong(DonutSlice::value).sum();
    List<LegendEntry> ageLegend = toLegend(ageData, ageTotal);

    // ── 프로그램별 참여 현황 (상위 6) ─────────────────────────
    List<ProgramStatRow> programStats =
        scoped.stream()
            .sorted(
                Comparator.comparing(
                    Program::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(6)
            .map(p -> toProgramStatRow(p))
            .toList();

    // ── 마감 임박 (applyEndDate 우선 · Qn-10) ─────────────────
    LocalDate cutoff = today.plusDays(7);
    List<Program> urgent =
        scoped.stream()
            .filter(p -> deadlineOf(p) != null)
            .filter(p -> !deadlineOf(p).isBefore(today) && !deadlineOf(p).isAfter(cutoff))
            .sorted(Comparator.comparing(this::deadlineOf))
            .limit(5)
            .toList();

    // ── 승인 대기 (상위 5) ────────────────────────────────────
    List<Long> ids = scoped.stream().map(Program::getId).toList();
    List<Application> allApp = applicationRepository.findAll();
    List<Application> pendingList =
        allApp.stream()
            .filter(a -> a.getStatus() == ApplicationStatus.PENDING)
            .filter(a -> a.getProgram() != null && ids.contains(a.getProgram().getId()))
            .sorted(
                Comparator.comparing(
                    Application::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .limit(5)
            .toList();

    return StatsModel.builder()
        .chartMode(yearMode ? "year" : "month")
        .chartSvg(chartSvg)
        .visitorsRangeLabel(rangeLabel(yearMode, today))
        .totalPrograms(totalPrograms)
        .totalUsers(totalUsers)
        .pendingCount(pending)
        .visitors30(visitors30)
        .visitorsDeltaPct(visitorsDeltaPct)
        .avgVisits(avgVisits)
        .peakVisits(peakVisits)
        .totalVisitsPeriod(totalVisits)
        .genderChartSvg(genderChartSvg)
        .genderLegend(genderLegend)
        .genderTotal(genderTotal)
        .ageChartSvg(ageChartSvg)
        .ageLegend(ageLegend)
        .ageTotal(ageTotal)
        .programStats(programStats)
        .urgentPrograms(urgent)
        .pendingApplications(pendingList)
        .build();
  }

  // ─────────────────────────────────────────────────────────────
  //   helpers
  // ─────────────────────────────────────────────────────────────

  private List<Program> scopedPrograms(Long scopeCenterId) {
    Stream<Program> all = programRepository.findAll().stream();
    if (scopeCenterId != null) {
      // A9-a: Center FK 기반. center 미할당(null) 프로그램은 스코프에서 제외.
      all = all.filter(p -> p.getCenter() != null && scopeCenterId.equals(p.getCenter().getId()));
    }
    return all.toList();
  }

  private List<User> scopedUsers(Long scopeCenterId) {
    List<User> all = userRepository.findAll();
    if (scopeCenterId == null) return all;
    return all.stream()
        .filter(u -> u.getCenter() != null && scopeCenterId.equals(u.getCenter().getId()))
        .toList();
  }

  private long countPendingApplications(List<Program> scoped) {
    if (scoped.isEmpty()) return 0L;
    List<Long> ids = scoped.stream().map(Program::getId).toList();
    return applicationRepository.findAll().stream()
        .filter(a -> a.getStatus() == ApplicationStatus.PENDING)
        .filter(a -> a.getProgram() != null && ids.contains(a.getProgram().getId()))
        .count();
  }

  private LocalDate deadlineOf(Program p) {
    return p.getApplyEndDate() != null ? p.getApplyEndDate() : p.getEndDate();
  }

  /** 최근 N일 간의 방문자 시리즈. 없는 날은 0. */
  private SeriesData dailySeries(LocalDate today, int days) {
    LocalDate from = today.minusDays(days - 1L);
    Map<LocalDate, Long> map = new HashMap<>();
    for (DailyVisit v :
        dailyVisitRepository.findByVisitDateBetweenOrderByVisitDateAsc(from, today)) {
      map.put(v.getVisitDate(), (long) v.getUniqueVisitors());
    }
    List<String> labels = new ArrayList<>(days);
    List<Long> values = new ArrayList<>(days);
    for (int i = 0; i < days; i++) {
      LocalDate d = from.plusDays(i);
      labels.add(d.format(MONTH_LABEL));
      values.add(map.getOrDefault(d, 0L));
    }
    long max = values.stream().mapToLong(Long::longValue).max().orElse(0);
    return new SeriesData(labels, values, Math.max(max, 100));
  }

  /** 최근 7년 (연도별 uniqueVisitors 합). */
  private SeriesData yearlySeries(LocalDate today) {
    int currentYear = today.getYear();
    List<String> labels = new ArrayList<>(7);
    List<Long> values = new ArrayList<>(7);
    for (int i = 6; i >= 0; i--) {
      int y = currentYear - i;
      LocalDate from = LocalDate.of(y, 1, 1);
      LocalDate to = LocalDate.of(y, 12, 31);
      long sum =
          dailyVisitRepository.findByVisitDateBetweenOrderByVisitDateAsc(from, to).stream()
              .mapToLong(DailyVisit::getUniqueVisitors)
              .sum();
      labels.add(String.valueOf(y));
      values.add(sum);
    }
    long max = values.stream().mapToLong(Long::longValue).max().orElse(0);
    return new SeriesData(labels, values, Math.max(max, 100));
  }

  private long sumVisitors(LocalDate from, LocalDate to) {
    return dailyVisitRepository.findByVisitDateBetweenOrderByVisitDateAsc(from, to).stream()
        .mapToLong(DailyVisit::getUniqueVisitors)
        .sum();
  }

  private static int pctDelta(long cur, long prev) {
    if (prev == 0) return cur == 0 ? 0 : 100;
    return (int) Math.round(((cur - prev) * 100.0) / prev);
  }

  private String rangeLabel(boolean yearMode, LocalDate today) {
    if (yearMode) {
      return (today.getYear() - 6) + " — " + today.getYear();
    }
    LocalDate from = today.minusDays(29);
    return from.format(DATE_LABEL) + " — " + today.format(DATE_LABEL);
  }

  private List<LegendEntry> toLegend(List<DonutSlice> data, long total) {
    List<LegendEntry> out = new ArrayList<>(data.size());
    for (DonutSlice s : data) {
      int pct = total == 0 ? 0 : (int) Math.round((s.value() * 100.0) / total);
      out.add(new LegendEntry(s.label(), s.color(), pct + "%", s.value()));
    }
    return out;
  }

  private ProgramStatRow toProgramStatRow(Program p) {
    long applied = applicationRepository.countByProgramId(p.getId());
    Integer cap = p.getCapacity();
    int pct = 0;
    if (cap != null && cap > 0) {
      pct = Math.min(100, (int) Math.round((applied * 100.0) / cap));
    }
    String progressColor;
    if (pct >= 80) progressColor = "#EF4444"; // 빨강
    else if (pct >= 50) progressColor = "#F59E0B"; // 주황
    else progressColor = SvgChartRenderer.COLOR_PRIMARY;
    String period = "";
    if (p.getStartDate() != null && p.getEndDate() != null) {
      period = p.getStartDate() + "\n~ " + p.getEndDate();
    }
    return ProgramStatRow.builder()
        .id(p.getId())
        .title(p.getTitle())
        // A9-a: DTO 필드명 organization → centerName. 값은 Center FK 우선, 없으면 organization fallback.
        .centerName(p.getCenter() != null ? p.getCenter().getName() : p.getOrganization())
        .period(period)
        .applied(applied)
        .capacity(cap == null ? 0 : cap)
        .capacityPct(pct)
        .progressColor(progressColor)
        .views(0) // Program.viewCount 미도입 (deferred: A6-followup)
        .status(p.getStatus())
        .build();
  }

  // ─────────────────────────────────────────────────────────────
  //   DTO
  // ─────────────────────────────────────────────────────────────

  @Getter
  @Builder
  public static class StatsModel {
    private String chartMode;
    private String chartSvg;
    private String visitorsRangeLabel;
    private long totalPrograms;
    private long totalUsers;
    private long pendingCount;
    private long visitors30;
    private int visitorsDeltaPct;
    private long avgVisits;
    private long peakVisits;
    private long totalVisitsPeriod;
    private String genderChartSvg;
    private List<LegendEntry> genderLegend;
    private long genderTotal;
    private String ageChartSvg;
    private List<LegendEntry> ageLegend;
    private long ageTotal;
    private List<ProgramStatRow> programStats;
    private List<Program> urgentPrograms;
    private List<Application> pendingApplications;
  }

  public record LegendEntry(String label, String color, String pct, long value) {}

  @Getter
  @Builder
  public static class ProgramStatRow {
    private Long id;
    private String title;
    // A9-a (2026-09-21): organization → centerName 리네임 (Q-A9-5 결정)
    private String centerName;
    private String period;
    private long applied;
    private int capacity;
    private int capacityPct;
    private String progressColor;
    private int views;
    private ProgramStatus status;
  }
}
