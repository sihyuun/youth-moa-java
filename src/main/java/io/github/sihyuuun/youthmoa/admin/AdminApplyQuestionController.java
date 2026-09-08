package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.program.ApplyQuestion;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.QuestionType;
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
 * F0c-dynamic-fields (2026-09-08 · Qn-1 A · Qn-8 C · Qn-9 A): 관리자 동적 신청 필드 CRUD.
 *
 * <ul>
 *   <li>SYSTEM_ADMIN 만 (@PreAuthorize)
 *   <li>soft delete only — {@code POST /admin/programs/{id}/dynamic-fields/{fieldId}/deactivate}
 *   <li>Qn-9 A: A3 미착수라 프로그램 편집 페이지 인라인 대신 별도 URL 로 진입
 * </ul>
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/programs/{programId}/dynamic-fields")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
public class AdminApplyQuestionController {

  private final AdminApplyQuestionService adminApplyQuestionService;
  private final AdminScope adminScope;

  // ================= 목록 =================

  @GetMapping
  public String list(@PathVariable Long programId, Model model) {
    Program program = loadProgramOr404(programId);
    List<ApplyQuestion> questions = adminApplyQuestionService.list(programId);
    populateCommonModel(model);
    model.addAttribute("program", program);
    model.addAttribute("questions", questions);
    return "admin/program-dynamic-field/list";
  }

  // ================= 신규 =================

  @GetMapping("/new")
  public String createForm(@PathVariable Long programId, Model model) {
    Program program = loadProgramOr404(programId);
    populateCommonModel(model);
    model.addAttribute("program", program);
    model.addAttribute("mode", "new");
    model.addAttribute("question", null);
    model.addAttribute("optionsText", "");
    model.addAttribute("answerCount", 0L);
    model.addAttribute("types", adminApplyQuestionService.allTypes());
    return "admin/program-dynamic-field/form";
  }

  @PostMapping
  public String create(
      @PathVariable Long programId,
      @RequestParam String fieldType,
      @RequestParam String label,
      @RequestParam(required = false, defaultValue = "false") boolean isRequired,
      @RequestParam(required = false, defaultValue = "1") int sortOrder,
      @RequestParam(required = false) String options,
      @RequestParam(required = false) Integer maxLength) {
    loadProgramOr404(programId);
    QuestionType type = parseType(fieldType);
    ApplyQuestion saved =
        adminApplyQuestionService.create(
            programId, type, label, isRequired, sortOrder, options, maxLength);
    return "redirect:/admin/programs/" + programId + "/dynamic-fields/" + saved.getId();
  }

  // ================= 편집 =================

  @GetMapping("/{fieldId}")
  public String editForm(@PathVariable Long programId, @PathVariable Long fieldId, Model model) {
    Program program = loadProgramOr404(programId);
    ApplyQuestion question;
    try {
      question = adminApplyQuestionService.findById(fieldId);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "동적 필드를 찾을 수 없어요.");
    }
    if (!question.getProgram().getId().equals(programId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "해당 프로그램의 필드가 아니에요.");
    }
    populateCommonModel(model);
    model.addAttribute("program", program);
    model.addAttribute("mode", "edit");
    model.addAttribute("question", question);
    model.addAttribute("optionsText", adminApplyQuestionService.optionsAsText(question));
    model.addAttribute("answerCount", adminApplyQuestionService.countAnswers(fieldId));
    model.addAttribute("types", adminApplyQuestionService.allTypes());
    return "admin/program-dynamic-field/form";
  }

  @PostMapping("/{fieldId}")
  public String update(
      @PathVariable Long programId,
      @PathVariable Long fieldId,
      @RequestParam String fieldType,
      @RequestParam String label,
      @RequestParam(required = false, defaultValue = "false") boolean isRequired,
      @RequestParam(required = false, defaultValue = "1") int sortOrder,
      @RequestParam(required = false) String options,
      @RequestParam(required = false) Integer maxLength) {
    loadProgramOr404(programId);
    QuestionType type = parseType(fieldType);
    adminApplyQuestionService.update(
        fieldId, type, label, isRequired, sortOrder, options, maxLength);
    return "redirect:/admin/programs/" + programId + "/dynamic-fields/" + fieldId;
  }

  /** Qn-8 C: soft delete. hard delete endpoint 는 신설하지 않는다. */
  @PostMapping("/{fieldId}/deactivate")
  public String deactivate(@PathVariable Long programId, @PathVariable Long fieldId) {
    loadProgramOr404(programId);
    adminApplyQuestionService.deactivate(fieldId);
    return "redirect:/admin/programs/" + programId + "/dynamic-fields";
  }

  @PostMapping("/{fieldId}/reactivate")
  public String reactivate(@PathVariable Long programId, @PathVariable Long fieldId) {
    loadProgramOr404(programId);
    adminApplyQuestionService.reactivate(fieldId);
    return "redirect:/admin/programs/" + programId + "/dynamic-fields";
  }

  // ================= 헬퍼 =================

  private Program loadProgramOr404(Long programId) {
    try {
      return adminApplyQuestionService.findProgram(programId);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "프로그램을 찾을 수 없어요.");
    }
  }

  private QuestionType parseType(String raw) {
    if (raw == null) throw new IllegalArgumentException("필드 타입을 선택해주세요.");
    try {
      return QuestionType.valueOf(raw.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("알 수 없는 필드 타입이에요: " + raw);
    }
  }

  private void populateCommonModel(Model model) {
    model.addAttribute("centerScopeLabel", adminScope.centerScopeLabel());
    model.addAttribute("isSystemAdmin", adminScope.isSystemAdmin());
    model.addAttribute("currentPage", "dynamic-fields");
  }
}
