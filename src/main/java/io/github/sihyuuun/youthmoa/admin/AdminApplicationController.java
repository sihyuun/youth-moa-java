package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.application.ApplyAnswer;
import io.github.sihyuuun.youthmoa.program.Program;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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
 * A4 admin-program-detail (2026-09-15 · Qn 24건 모두 A): 관리자 프로그램 신청 관리.
 *
 * <ul>
 *   <li>GET / — 목록 (필터·페이지네이션·요약 배지)
 *   <li>GET /{aid} — 상세 모달 fragment
 *   <li>POST /{aid}/approve — 승인
 *   <li>POST /{aid}/reject — 반려 (사유 필수)
 *   <li>POST /{aid}/cancel — 관리자 강제 취소 (사유 필수)
 *   <li>POST /{aid}/note — 담당자 의견 저장
 * </ul>
 *
 * <p>Qn-A A: SYSTEM_ADMIN + CENTER_ADMIN. AdminScope 문자열 매칭.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/programs/{programId}/applications")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CENTER_ADMIN')")
public class AdminApplicationController {

  private final AdminApplicationService adminApplicationService;
  private final AdminApplicationBulkService adminApplicationBulkService;
  private final AdminScope adminScope;

  // ================= 목록 =================

  @GetMapping
  public String list(
      @PathVariable Long programId,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "0") int page,
      Model model) {
    Program program;
    try {
      program = adminApplicationService.assertProgramInScope(programId);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로그램을 찾을 수 없어요.");
    } catch (IllegalAccessError e) {
      throw new AccessDeniedException(e.getMessage());
    }

    Page<Application> applications = adminApplicationService.list(programId, status, q, page);

    // 참여횟수 (visits) 파생 — userId 일괄 조회로 N+1 방지
    List<Long> userIds = applications.getContent().stream().map(a -> a.getUser().getId()).toList();
    Map<Long, Long> visits = adminApplicationService.visitsByUserIds(userIds);

    // 요약 카운트
    Map<ApplicationStatus, Long> summary = adminApplicationService.summaryCounts(programId);
    long total = adminApplicationService.totalCount(programId);

    populateCommonModel(model);
    model.addAttribute("program", program);
    model.addAttribute("applications", applications);
    model.addAttribute("visits", visits);
    model.addAttribute("summary", summary);
    model.addAttribute("total", total);
    model.addAttribute("statusFilter", status == null ? "" : status);
    model.addAttribute("q", q == null ? "" : q);
    model.addAttribute("statusOptions", adminApplicationService.statusOptions());

    // 페이지 그룹 (5개 단위) — AdminProgramController 패턴
    int groupSize = 5;
    int totalPages = Math.max(1, applications.getTotalPages());
    int current = applications.getNumber();
    int groupStart = (current / groupSize) * groupSize;
    int groupEnd = Math.min(totalPages - 1, groupStart + groupSize - 1);
    model.addAttribute("pageGroupStart", groupStart);
    model.addAttribute("pageGroupEnd", groupEnd);
    model.addAttribute("hasPrevGroup", groupStart > 0);
    model.addAttribute("hasNextGroup", groupEnd < totalPages - 1);
    model.addAttribute("prevGroupPage", Math.max(0, groupStart - 1));
    model.addAttribute("nextGroupPage", Math.min(totalPages - 1, groupEnd + 1));

    return "admin/program/applications";
  }

  // ================= 상세 모달 fragment =================

  @GetMapping("/{aid}")
  public String detail(@PathVariable Long programId, @PathVariable Long aid, Model model) {
    Application app;
    try {
      app = adminApplicationService.findById(programId, aid);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "신청을 찾을 수 없어요.");
    } catch (IllegalAccessError e) {
      throw new AccessDeniedException(e.getMessage());
    }
    List<ApplyAnswer> answers = adminApplicationService.findAnswers(aid);
    // 참여횟수
    Map<Long, Long> visits =
        adminApplicationService.visitsByUserIds(List.of(app.getUser().getId()));
    model.addAttribute("app", app);
    model.addAttribute("answers", answers);
    model.addAttribute("visits", visits);
    model.addAttribute("programId", programId);
    return "admin/program/_application-detail-modal :: modal";
  }

  // ================= 상태 변경 =================

  @PostMapping("/{aid}/approve")
  public String approve(
      @PathVariable Long programId,
      @PathVariable Long aid,
      @AuthenticationPrincipal io.github.sihyuuun.youthmoa.user.UserPrincipal principal,
      RedirectAttributes ra) {
    try {
      adminApplicationService.approve(programId, aid, principal.getUsername());
      ra.addFlashAttribute("flashMessage", "신청을 승인했어요.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    } catch (IllegalAccessError e) {
      throw new AccessDeniedException(e.getMessage());
    }
    return "redirect:/admin/programs/" + programId + "/applications";
  }

  @PostMapping("/{aid}/reject")
  public String reject(
      @PathVariable Long programId,
      @PathVariable Long aid,
      @RequestParam(required = false) String rejectReason,
      @AuthenticationPrincipal io.github.sihyuuun.youthmoa.user.UserPrincipal principal,
      RedirectAttributes ra) {
    try {
      adminApplicationService.reject(programId, aid, principal.getUsername(), rejectReason);
      ra.addFlashAttribute("flashMessage", "신청을 반려했어요.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    } catch (IllegalAccessError e) {
      throw new AccessDeniedException(e.getMessage());
    }
    return "redirect:/admin/programs/" + programId + "/applications";
  }

  @PostMapping("/{aid}/cancel")
  public String cancel(
      @PathVariable Long programId,
      @PathVariable Long aid,
      @RequestParam(required = false) String cancelReason,
      @AuthenticationPrincipal io.github.sihyuuun.youthmoa.user.UserPrincipal principal,
      RedirectAttributes ra) {
    try {
      adminApplicationService.forceCancel(programId, aid, principal.getUsername(), cancelReason);
      ra.addFlashAttribute("flashMessage", "신청을 취소했어요.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    } catch (IllegalAccessError e) {
      throw new AccessDeniedException(e.getMessage());
    }
    return "redirect:/admin/programs/" + programId + "/applications";
  }

  @PostMapping("/{aid}/note")
  public String updateNote(
      @PathVariable Long programId,
      @PathVariable Long aid,
      @RequestParam(required = false) String adminNote,
      RedirectAttributes ra) {
    try {
      adminApplicationService.updateNote(programId, aid, adminNote);
      ra.addFlashAttribute("flashMessage", "담당자 의견을 저장했어요.");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    } catch (IllegalAccessError e) {
      throw new AccessDeniedException(e.getMessage());
    }
    return "redirect:/admin/programs/" + programId + "/applications";
  }

  // ================= A8 admin-bulk (2026-09-17 · Qn-App1 A: Approve 만) =================

  @PostMapping("/bulk/approve")
  public String bulkApprove(
      @PathVariable Long programId,
      @RequestParam(value = "ids", required = false) List<Long> ids,
      @AuthenticationPrincipal io.github.sihyuuun.youthmoa.user.UserPrincipal principal,
      RedirectAttributes ra) {
    try {
      BulkResult result =
          adminApplicationBulkService.bulkApprove(programId, ids, principal.getUsername());
      applyBulkFlash(ra, result, "승인");
    } catch (IllegalArgumentException e) {
      ra.addFlashAttribute("flashError", e.getMessage());
    } catch (IllegalStateException e) {
      throw new AccessDeniedException(e.getMessage());
    }
    return "redirect:/admin/programs/" + programId + "/applications";
  }

  private static void applyBulkFlash(RedirectAttributes ra, BulkResult result, String actionLabel) {
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

  // ================= 헬퍼 =================

  private void populateCommonModel(Model model) {
    model.addAttribute("centerScopeLabel", adminScope.centerScopeLabel());
    model.addAttribute("isSystemAdmin", adminScope.isSystemAdmin());
    model.addAttribute("currentPage", "programs");
  }
}
