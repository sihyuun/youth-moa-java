package io.github.sihyuuun.youthmoa.admin;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/**
 * A2 (2026-09-09 · Qn-1~9 모두 A): 관리자 프로그램 목록 · 상세 조회 컨트롤러.
 *
 * <ul>
 *   <li>Qn-1 A — SYSTEM_ADMIN + CENTER_ADMIN 두 role 허용. 스코프 필터는 {@link AdminProgramService} 에 강제.
 *   <li>Qn-2 A — 관리자 전용 상세 페이지 신설 (사용자 사이드 재활용 안 함)
 *   <li>Qn-3 A — 필터 5종 · 검색 · 컬럼 9종 (applyPeriod/views 는 "-" 자리)
 *   <li>Qn-4 A — 페이지당 10건
 *   <li>Qn-5 A — createdAt DESC
 *   <li>Qn-7 A — 액션 컬럼 "편집" 링크만 (상세 페이지로 이동, 실 편집은 A3)
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

  @GetMapping("/{id}")
  public String detail(@PathVariable Long id, Model model) {
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
    model.addAttribute("program", program);
    model.addAttribute("applied", applied);
    return "admin/program/detail";
  }

  private void populateCommonModel(Model model) {
    model.addAttribute("centerScopeLabel", adminScope.centerScopeLabel());
    model.addAttribute("isSystemAdmin", adminScope.isSystemAdmin());
    model.addAttribute("currentPage", "programs");
  }
}
