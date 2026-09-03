package io.github.sihyuuun.youthmoa.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AdminDashboardController {

  private final AdminScope adminScope;
  private final AdminDashboardService dashboardService;

  @GetMapping("/admin")
  public String dashboard(Model model) {
    String scopeCenter = adminScope.effectiveCenterName();
    AdminDashboardService.DashboardModel data = dashboardService.load(scopeCenter);
    model.addAttribute("dashboard", data);
    model.addAttribute("centerScopeLabel", adminScope.centerScopeLabel());
    model.addAttribute("isSystemAdmin", adminScope.isSystemAdmin());
    model.addAttribute("currentPage", "dashboard");
    return "admin/dashboard";
  }
}
