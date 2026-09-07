package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.user.Term;
import java.util.List;
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

/**
 * A-admin-terms-crud (2026-09-04 · Qn-1 B · Qn-2 B · Qn-3~9 A): 관리자 약관 CRUD 컨트롤러.
 *
 * <ul>
 *   <li>GET /admin/terms — 목록 (SYSTEM_ADMIN + CENTER_ADMIN 조회 가능, Qn-1 B)
 *   <li>GET /admin/terms/new — 신규 폼 (SYSTEM_ADMIN 만)
 *   <li>POST /admin/terms — Create (SYSTEM_ADMIN 만)
 *   <li>GET /admin/terms/{id} — 편집 폼 (SYSTEM_ADMIN + CENTER_ADMIN 조회)
 *   <li>POST /admin/terms/{id} — Update (SYSTEM_ADMIN 만, Qn-6 bumpVersion 옵션)
 *   <li>POST /admin/terms/{id}/delete — Delete (SYSTEM_ADMIN 만, Qn-3 FK 참조 시 400)
 * </ul>
 *
 * <p>RBAC 는 {@code @PreAuthorize("hasRole('SYSTEM_ADMIN')")} 로 UD 만 제한. 조회는 SecurityConfig admin
 * chain 의 {@code hasAnyRole("CENTER_ADMIN","SYSTEM_ADMIN")} 매처로 커버.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/terms")
public class AdminTermController {

  private final AdminTermService adminTermService;
  private final AdminScope adminScope;

  // ================= 목록 =================

  @GetMapping
  public String list(Model model) {
    List<Term> terms = adminTermService.list();
    populateCommonModel(model);
    model.addAttribute("terms", terms);
    model.addAttribute("activeRequiredCount", adminTermService.countActiveRequired());
    return "admin/term/list";
  }

  // ================= 신규 =================

  @GetMapping("/new")
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public String createForm(Model model) {
    populateCommonModel(model);
    model.addAttribute("mode", "new");
    model.addAttribute("term", null);
    return "admin/term/form";
  }

  @PostMapping
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public String create(
      @RequestParam String code,
      @RequestParam String title,
      @RequestParam String contentPath,
      @RequestParam String content,
      @RequestParam(required = false, defaultValue = "false") boolean required,
      @RequestParam(required = false, defaultValue = "1") int sortOrder,
      @RequestParam(required = false, defaultValue = "false") boolean isActive) {
    Term saved =
        adminTermService.create(code, title, contentPath, content, required, sortOrder, isActive);
    return "redirect:/admin/terms/" + saved.getId();
  }

  // ================= 편집 =================

  @GetMapping("/{id}")
  public String editForm(@PathVariable Long id, Model model) {
    Term term;
    try {
      term = adminTermService.findById(id);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "약관을 찾을 수 없어요.");
    }
    populateCommonModel(model);
    model.addAttribute("mode", "edit");
    model.addAttribute("term", term);
    return "admin/term/form";
  }

  @PostMapping("/{id}")
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public String update(
      @PathVariable Long id,
      @RequestParam String title,
      @RequestParam String contentPath,
      @RequestParam String content,
      @RequestParam(required = false, defaultValue = "false") boolean required,
      @RequestParam(required = false, defaultValue = "1") int sortOrder,
      @RequestParam(required = false, defaultValue = "false") boolean isActive,
      @RequestParam(required = false, defaultValue = "false") boolean bumpVersion) {
    adminTermService.update(
        id, title, contentPath, content, required, sortOrder, isActive, bumpVersion);
    return "redirect:/admin/terms/" + id;
  }

  @PostMapping("/{id}/delete")
  @PreAuthorize("hasRole('SYSTEM_ADMIN')")
  public String delete(@PathVariable Long id) {
    adminTermService.delete(id);
    return "redirect:/admin/terms";
  }

  // ================= 헬퍼 =================

  private void populateCommonModel(Model model) {
    model.addAttribute("centerScopeLabel", adminScope.centerScopeLabel());
    model.addAttribute("isSystemAdmin", adminScope.isSystemAdmin());
    model.addAttribute("currentPage", "terms");
  }
}
