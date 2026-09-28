package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.notification.watch.ProgramWatch;
import io.github.sihyuuun.youthmoa.notification.watch.ProgramWatchService;
import io.github.sihyuuun.youthmoa.program.Program;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

  private final AdminScope adminScope;
  private final AdminDashboardService dashboardService;
  private final ProgramWatchService programWatchService;

  @GetMapping("/admin")
  public String dashboard(@AuthenticationPrincipal UserDetails principal, Model model) {
    Long scopeCenterId = adminScope.effectiveCenterId();
    AdminDashboardService.DashboardModel data = dashboardService.load(scopeCenterId);

    // A7-watcher-ui (2026-09-28 · Q-A7W-3 b): 지켜보는 프로그램 최근 5.
    // findWatchedPrograms 는 EAGER fetch 로 program + center 를 즉시 로드 → 카드 렌더 N+1 회피.
    List<Program> watched =
        programWatchService
            .findWatchedPrograms(
                principal != null ? principal.getUsername() : null, PageRequest.of(0, 5))
            .map(ProgramWatch::getProgram)
            .getContent();

    AdminDashboardService.DashboardModel augmented =
        AdminDashboardService.DashboardModel.builder()
            .activeCount(data.getActiveCount())
            .closedCount(data.getClosedCount())
            .upcomingCount(data.getUpcomingCount())
            .totalUsers(data.getTotalUsers())
            .pendingCount(data.getPendingCount())
            .recentPrograms(data.getRecentPrograms())
            .urgentPrograms(data.getUrgentPrograms())
            .watchedPrograms(watched)
            .build();

    model.addAttribute("dashboard", augmented);
    model.addAttribute("centerScopeLabel", adminScope.centerScopeLabel());
    model.addAttribute("isSystemAdmin", adminScope.isSystemAdmin());
    model.addAttribute("currentPage", "dashboard");
    return "admin/dashboard";
  }
}
