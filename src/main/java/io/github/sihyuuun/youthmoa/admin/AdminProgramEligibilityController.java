package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramEligibility;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * F4-admin-eligibility (2026-09-09 · Qn-1~8 모두 A): 관리자 프로그램 자격요건 편집.
 *
 * <ul>
 *   <li>Qn-1 A — SYSTEM_ADMIN 만 (@PreAuthorize)
 *   <li>Qn-2 A — 3필드 공란 저장 = 삭제 (별도 endpoint 없음)
 *   <li>Qn-5 A — A3 미착수라 프로그램 편집 페이지 인라인 대신 별도 URL 진입
 *   <li>Qn-8 A — PRG redirect (F0c / notice / term 승계)
 * </ul>
 *
 * <p>스키마 변경 없음 — {@link ProgramEligibility} 는 F4-detail (PR #73) 에서 이미 신설된 @Embedded 값 객체.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/programs/{programId}/eligibility")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminProgramEligibilityController {

  private final AdminProgramEligibilityService adminProgramEligibilityService;
  private final AdminScope adminScope;

  @GetMapping
  public String editForm(@PathVariable Long programId, Model model) {
    Program program = loadProgramOr404(programId);
    ProgramEligibility eligibility = program.getEligibility();
    populateCommonModel(model);
    model.addAttribute("program", program);
    model.addAttribute("age", eligibility != null ? eligibility.getAge() : "");
    model.addAttribute("region", eligibility != null ? eligibility.getRegion() : "");
    model.addAttribute("etc", eligibility != null ? eligibility.getEtc() : "");
    return "admin/program-eligibility/form";
  }

  @PostMapping
  public String update(
      @PathVariable Long programId,
      @RequestParam(required = false) String age,
      @RequestParam(required = false) String region,
      @RequestParam(required = false) String etc,
      RedirectAttributes redirectAttributes) {
    loadProgramOr404(programId);
    adminProgramEligibilityService.update(programId, age, region, etc);
    redirectAttributes.addFlashAttribute("flashMessage", "자격요건을 저장했어요.");
    return "redirect:/admin/programs/" + programId + "/eligibility";
  }

  private Program loadProgramOr404(Long programId) {
    try {
      return adminProgramEligibilityService.findProgram(programId);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로그램을 찾을 수 없어요.");
    }
  }

  private void populateCommonModel(Model model) {
    model.addAttribute("centerScopeLabel", adminScope.centerScopeLabel());
    model.addAttribute("isSystemAdmin", adminScope.isSystemAdmin());
    model.addAttribute("currentPage", "eligibility");
  }
}
