package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.program.ApprovalMode;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramStatus;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * A2 (2026-09-09) + A3-1 (2026-09-10 · Qn-A/B/C/1~8/Δ1~6 모두 A): 관리자 프로그램 목록·조회 + 등록/편집/삭제.
 *
 * <ul>
 *   <li>A2 목록/상세 조회: SYSTEM_ADMIN + CENTER_ADMIN 모두 허용
 *   <li>A3-1 등록/편집/삭제: <b>SYSTEM_ADMIN only</b> (Qn-1 A). Program-Center FK 미도입 상태라 CENTER 격리
 *       fragile → A9 이후 CENTER_ADMIN 확장 예정
 *   <li>Qn-A A — {@code GET /admin/programs/{id}} 는 편집 폼으로 대체. 기존 read-only detail 페이지 폐기
 *   <li>Qn-B A — F4/F0c 는 편집 폼 상단 링크로 진입 (별도 페이지 유지, 인라인 통합은 A3-2)
 *   <li>Qn-5 A — 응답은 PRG redirect + flash (form 실패 시 400 매핑 = admin-notice 패턴)
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/programs")
@PreAuthorize("hasAnyRole('SYSTEM_ADMIN','CENTER_ADMIN')")
public class AdminProgramController {

  private final AdminProgramService adminProgramService;
  private final AdminScope adminScope;

  @GetMapping
  public String list(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String status,
      @RequestParam(required = false, defaultValue = "0") int page,
      Model model) {
    Page<Program> programs = adminProgramService.list(q, status, page);
    populateCommonModel(model);

    // 배치 조회 — 목록 신청수 N+1 방지
    List<Long> ids = programs.getContent().stream().map(Program::getId).toList();
    Map<Long, Long> appliedCounts = adminProgramService.countAppliedByProgramIds(ids);

    model.addAttribute("programs", programs);
    model.addAttribute("appliedCounts", appliedCounts);
    model.addAttribute("q", q == null ? "" : q);
    model.addAttribute("statusFilter", status == null ? "" : status);
    model.addAttribute("statusOptions", ProgramStatus.values());

    // 페이지 그룹 (5개 단위) — AdminNoticeController 패턴
    int groupSize = 5;
    int totalPages = Math.max(1, programs.getTotalPages());
    int current = programs.getNumber();
    int groupStart = (current / groupSize) * groupSize;
    int groupEnd = Math.min(totalPages - 1, groupStart + groupSize - 1);
    model.addAttribute("pageGroupStart", groupStart);
    model.addAttribute("pageGroupEnd", groupEnd);
    model.addAttribute("hasPrevGroup", groupStart > 0);
    model.addAttribute("hasNextGroup", groupEnd < totalPages - 1);
    model.addAttribute("prevGroupPage", Math.max(0, groupStart - 1));
    model.addAttribute("nextGroupPage", Math.min(totalPages - 1, groupEnd + 1));

    return "admin/program/list";
  }

  // ================= A3-1 신규 등록 =================

  @GetMapping("/new")
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public String createForm(Model model) {
    populateCommonModel(model);
    model.addAttribute("mode", "new");
    model.addAttribute("program", null);
    ProgramFormRequest form = new ProgramFormRequest();
    form.setApprovalMode(ApprovalMode.MANUAL);
    form.setActive(true);
    model.addAttribute("form", form);
    model.addAttribute("approvalModes", ApprovalMode.values());
    return "admin/program/form";
  }

  @PostMapping
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public String create(@ModelAttribute("form") ProgramFormRequest form) {
    Program saved = adminProgramService.create(form);
    return "redirect:/admin/programs/" + saved.getId();
  }

  // ================= A3-1 편집 (Qn-A A: 상세 = 편집 폼) =================

  @GetMapping("/{id}")
  public String editForm(@PathVariable Long id, Model model) {
    Program program;
    try {
      program = adminProgramService.find(id);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로그램을 찾을 수 없어요.");
    } catch (IllegalAccessError e) {
      throw new AccessDeniedException(e.getMessage());
    }
    long applied = adminProgramService.countApplied(program);
    populateCommonModel(model);
    model.addAttribute("mode", "edit");
    model.addAttribute("program", program);
    model.addAttribute("applied", applied);
    model.addAttribute("form", toForm(program));
    model.addAttribute("approvalModes", ApprovalMode.values());
    return "admin/program/form";
  }

  @PostMapping("/{id}")
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public String update(@PathVariable Long id, @ModelAttribute("form") ProgramFormRequest form) {
    adminProgramService.update(id, form);
    return "redirect:/admin/programs/" + id;
  }

  @PostMapping("/{id}/delete")
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public String delete(@PathVariable Long id) {
    adminProgramService.delete(id);
    return "redirect:/admin/programs";
  }

  // ================= 헬퍼 =================

  private ProgramFormRequest toForm(Program p) {
    ProgramFormRequest f = new ProgramFormRequest();
    f.setTitle(p.getTitle());
    f.setOrganization(p.getOrganization());
    f.setCategory(p.getCategory());
    f.setRegion(p.getRegion());
    f.setImageUrl(p.getImageUrl());
    f.setDescription(p.getDescription());
    f.setContent(p.getContent());
    f.setStartDate(p.getStartDate());
    f.setEndDate(p.getEndDate());
    f.setApplyStartDate(p.getApplyStartDate());
    f.setApplyEndDate(p.getApplyEndDate());
    f.setVenue(p.getVenue());
    f.setContact(p.getContact());
    f.setCapacity(p.getCapacity());
    f.setApprovalMode(p.getApprovalMode() != null ? p.getApprovalMode() : ApprovalMode.MANUAL);
    f.setTermsService(p.getTermsService());
    f.setTermsPrivacy(p.getTermsPrivacy());
    f.setTermsMarketing(p.getTermsMarketing());
    f.setActive(p.isActive());
    return f;
  }

  private void populateCommonModel(Model model) {
    model.addAttribute("centerScopeLabel", adminScope.centerScopeLabel());
    model.addAttribute("isSystemAdmin", adminScope.isSystemAdmin());
    model.addAttribute("currentPage", "programs");
  }
}
