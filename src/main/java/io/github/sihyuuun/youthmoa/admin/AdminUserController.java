package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * A5 admin-users (2026-09-15): 관리자 사용자 관리.
 *
 * <ul>
 *   <li>GET / — 목록 (검색·role filter·페이지네이션 10건)
 *   <li>GET /{uid} — 상세 (좌 프로필·우 신청 현황 4탭)
 *   <li>POST /{uid}/deactivate — 차단 (사유 필수)
 *   <li>POST /{uid}/reactivate — 재활성화
 *   <li>POST /{uid}/role — role 변경 (SYSTEM_ADMIN 전용)
 *   <li>POST /{uid}/admin-note — 관리자 메모
 * </ul>
 *
 * <p>Qn-A 이월: `/admin/staff` 별도 화면 X · SYSTEM_ADMIN 만 진입. CENTER_ADMIN 은 admin 트랙에서 사용자 관리 미노출 (헤더
 * GNB 도 없음).
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminUserController {

  private final AdminUserService adminUserService;
  private final AdminUserBulkService adminUserBulkService;
  private final AdminScope adminScope;
  private final UserRepository userRepository;

  // ================= 목록 =================

  @GetMapping
  public String list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String role,
      @RequestParam(required = false, defaultValue = "0") int page,
      Model model) {
    Page<User> users = adminUserService.list(q, role, page);

    populateCommonModel(model);
    model.addAttribute("users", users);
    model.addAttribute("q", q == null ? "" : q);
    model.addAttribute("roleFilter", role == null ? "" : role);
    model.addAttribute("roleOptions", adminUserService.roleOptions());

    // 페이지 그룹 (5개 단위) — AdminProgramController 패턴 통일
    int groupSize = 5;
    int totalPages = Math.max(1, users.getTotalPages());
    int current = users.getNumber();
    int groupStart = (current / groupSize) * groupSize;
    int groupEnd = Math.min(totalPages - 1, groupStart + groupSize - 1);
    model.addAttribute("pageGroupStart", groupStart);
    model.addAttribute("pageGroupEnd", groupEnd);
    model.addAttribute("hasPrevGroup", groupStart > 0);
    model.addAttribute("hasNextGroup", groupEnd < totalPages - 1);
    model.addAttribute("prevGroupPage", Math.max(0, groupStart - 1));
    model.addAttribute("nextGroupPage", Math.min(totalPages - 1, groupEnd + 1));

    return "admin/user/list";
  }

  // ================= 상세 =================

  @GetMapping("/{uid}")
  public String detail(
      @PathVariable Long uid,
      @RequestParam(required = false, defaultValue = "ALL") String tab,
      @AuthenticationPrincipal UserPrincipal principal,
      Model model) {
    User target;
    try {
      target = adminUserService.findById(uid);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
    List<Application> applications = adminUserService.findApplicationsByUser(target, tab);

    populateCommonModel(model);
    model.addAttribute("target", target);
    model.addAttribute("applications", applications);
    model.addAttribute("activeTab", tab);
    model.addAttribute("roleOptions", adminUserService.roleOptions());
    model.addAttribute("currentUserId", principal.getId());
    model.addAttribute(
        "isSelf",
        principal != null && principal.getId() != null && principal.getId().equals(target.getId()));
    return "admin/user/detail";
  }

  // ================= 액션 =================

  @PostMapping("/{uid}/deactivate")
  public String deactivate(
      @PathVariable Long uid,
      @RequestParam(required = false) String reason,
      @AuthenticationPrincipal UserPrincipal principal,
      RedirectAttributes ra) {
    try {
      User admin = loadCurrentAdmin(principal);
      adminUserService.deactivate(uid, admin, reason);
      ra.addFlashAttribute("flashMessage", "사용자를 차단했어요.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    }
    return "redirect:/admin/users/" + uid;
  }

  @PostMapping("/{uid}/reactivate")
  public String reactivate(
      @PathVariable Long uid,
      @AuthenticationPrincipal UserPrincipal principal,
      RedirectAttributes ra) {
    try {
      User admin = loadCurrentAdmin(principal);
      adminUserService.reactivate(uid, admin);
      ra.addFlashAttribute("flashMessage", "사용자를 재활성화했어요.");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    }
    return "redirect:/admin/users/" + uid;
  }

  @PostMapping("/{uid}/role")
  public String changeRole(
      @PathVariable Long uid,
      @RequestParam String role,
      @AuthenticationPrincipal UserPrincipal principal,
      RedirectAttributes ra) {
    try {
      User admin = loadCurrentAdmin(principal);
      UserRole newRole = UserRole.valueOf(role.toUpperCase());
      adminUserService.changeRole(uid, admin, newRole);
      ra.addFlashAttribute("flashMessage", "권한을 변경했어요.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("flashError", "권한 값이 올바르지 않아요.");
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    }
    return "redirect:/admin/users/" + uid;
  }

  @PostMapping("/{uid}/admin-note")
  public String updateAdminNote(
      @PathVariable Long uid,
      @RequestParam(required = false) String adminNote,
      RedirectAttributes ra) {
    try {
      adminUserService.updateAdminNote(uid, adminNote);
      ra.addFlashAttribute("flashMessage", "관리자 메모를 저장했어요.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    }
    return "redirect:/admin/users/" + uid;
  }

  // ================= A8 admin-bulk (2026-09-17) =================

  @PostMapping("/bulk/deactivate")
  public String bulkDeactivate(
      @RequestParam(value = "ids", required = false) List<Long> ids,
      @RequestParam(required = false) String reason,
      @AuthenticationPrincipal UserPrincipal principal,
      RedirectAttributes ra) {
    try {
      User admin = loadCurrentAdmin(principal);
      BulkResult result = adminUserBulkService.bulkDeactivate(ids, admin, reason);
      applyFlash(ra, result, "차단");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    }
    return "redirect:/admin/users";
  }

  @PostMapping("/bulk/reactivate")
  public String bulkReactivate(
      @RequestParam(value = "ids", required = false) List<Long> ids,
      @AuthenticationPrincipal UserPrincipal principal,
      RedirectAttributes ra) {
    try {
      User admin = loadCurrentAdmin(principal);
      BulkResult result = adminUserBulkService.bulkReactivate(ids, admin);
      applyFlash(ra, result, "재활성화");
    } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    }
    return "redirect:/admin/users";
  }

  @PostMapping("/bulk/role")
  public String bulkChangeRole(
      @RequestParam(value = "ids", required = false) List<Long> ids,
      @RequestParam String role,
      @AuthenticationPrincipal UserPrincipal principal,
      RedirectAttributes ra) {
    try {
      User admin = loadCurrentAdmin(principal);
      UserRole newRole = UserRole.valueOf(role.toUpperCase());
      BulkResult result = adminUserBulkService.bulkChangeRole(ids, admin, newRole);
      applyFlash(ra, result, "권한 변경");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("flashError", "권한 값이 올바르지 않아요.");
    } catch (IllegalStateException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    }
    return "redirect:/admin/users";
  }

  // ================= 헬퍼 =================

  private static void applyFlash(RedirectAttributes ra, BulkResult result, String actionLabel) {
    if (result.getTotal() == 0) {
      ra.addFlashAttribute("flashError", "선택된 항목이 없어요.");
      return;
    }
    String msg =
        "총 "
            + result.getTotal()
            + "건 중 "
            + result.getSuccessCount()
            + "건 "
            + actionLabel
            + "이 완료됐어요.";
    if (result.hasFailure()) {
      StringBuilder sb = new StringBuilder(msg);
      sb.append(" ").append(result.getFailCount()).append("건은 처리하지 못했어요");
      // 실패 5건까지 상세 표시
      int shown = 0;
      for (BulkResult.FailureRow f : result.getErrors()) {
        if (shown++ >= 5) break;
        sb.append(" · #").append(f.id()).append(" (").append(f.reason()).append(")");
      }
      ra.addFlashAttribute("flashError", sb.toString());
    } else {
      ra.addFlashAttribute("flashMessage", msg);
    }
  }

  private User loadCurrentAdmin(UserPrincipal principal) {
    return userRepository
        .findById(principal.getId())
        .orElseThrow(() -> new IllegalStateException("현재 관리자 정보를 확인할 수 없어요."));
  }

  private void populateCommonModel(Model model) {
    model.addAttribute("centerScopeLabel", adminScope.centerScopeLabel());
    model.addAttribute("isSystemAdmin", adminScope.isSystemAdmin());
    model.addAttribute("currentPage", "users");
  }
}
