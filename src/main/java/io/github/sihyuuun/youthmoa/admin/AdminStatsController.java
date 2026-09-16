package io.github.sihyuuun.youthmoa.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A6 admin-stats (2026-09-16 신설) — {@code /admin/stats} 라우트.
 *
 * <p>KPI · 방문자 라인/막대 · 프로그램별 참여 · 성별/연령 도넛 · 마감임박 · 승인대기.
 *
 * <p>HTMX 부분 갱신: {@code /admin/stats/chart?mode=year|month} 는 fragment 만 반환.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')")
public class AdminStatsController {

  private final AdminScope adminScope;
  private final AdminStatsService statsService;

  @GetMapping("/admin/stats")
  public String stats(
      @RequestParam(name = "chartMode", required = false, defaultValue = "month") String chartMode,
      Model model) {
    String scope = adminScope.effectiveCenterName();
    AdminStatsService.StatsModel data = statsService.load(scope, chartMode);
    model.addAttribute("stats", data);
    model.addAttribute("centerScopeLabel", adminScope.centerScopeLabel());
    model.addAttribute("isSystemAdmin", adminScope.isSystemAdmin());
    model.addAttribute("currentPage", "stats");
    return "admin/stats";
  }

  /** HTMX 방문자 차트만 재렌더. hx-target="#chart-container" hx-swap="innerHTML". */
  @GetMapping("/admin/stats/chart")
  public String chartFragment(
      @RequestParam(name = "mode", required = false, defaultValue = "month") String mode,
      Model model) {
    String scope = adminScope.effectiveCenterName();
    AdminStatsService.StatsModel data = statsService.load(scope, mode);
    model.addAttribute("stats", data);
    return "admin/stats :: chart";
  }
}
