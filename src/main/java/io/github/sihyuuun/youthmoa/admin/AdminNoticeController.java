package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.notice.NoticeAttachment;
import io.github.sihyuuun.youthmoa.notice.NoticeCategory;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * A-admin-notice-attachment (2026-09-03 · Qn-5 A · Qn-7 A · Qn-8 Custom): 관리자 공지 CRUD + 첨부 관리.
 *
 * <ul>
 *   <li>목록 · 신규 · 편집 · 삭제 (표준 SSR + PRG)
 *   <li>첨부 업로드 / 삭제 (HTMX fragment outerHTML swap)
 * </ul>
 *
 * <p>RBAC 는 {@link AdminNoticeService#canEdit} 에서 판정. Update/Delete 진입 시 SYSTEM_ADMIN 아니면 본인 작성만.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/notices")
public class AdminNoticeController {

  private final AdminNoticeService adminNoticeService;
  private final AdminScope adminScope;
  private final UserRepository userRepository;

  // ================= 목록 =================

  @GetMapping
  public String list(@RequestParam(required = false, defaultValue = "0") int page, Model model) {
    Page<Notice> notices = adminNoticeService.list(page);
    populateCommonModel(model, "notices");
    model.addAttribute("notices", notices);

    // 페이지 그룹 (5개 단위)
    int groupSize = 5;
    int totalPages = Math.max(1, notices.getTotalPages());
    int current = notices.getNumber();
    int groupStart = (current / groupSize) * groupSize;
    int groupEnd = Math.min(totalPages - 1, groupStart + groupSize - 1);
    model.addAttribute("pageGroupStart", groupStart);
    model.addAttribute("pageGroupEnd", groupEnd);
    model.addAttribute("hasPrevGroup", groupStart > 0);
    model.addAttribute("hasNextGroup", groupEnd < totalPages - 1);
    model.addAttribute("prevGroupPage", Math.max(0, groupStart - 1));
    model.addAttribute("nextGroupPage", Math.min(totalPages - 1, groupEnd + 1));

    return "admin/notice/list";
  }

  // ================= 신규 =================

  @GetMapping("/new")
  public String createForm(Model model) {
    populateCommonModel(model, "notices");
    model.addAttribute("mode", "new");
    model.addAttribute("notice", null);
    model.addAttribute("attachments", List.of());
    model.addAttribute("categories", NoticeCategory.values());
    model.addAttribute("canEdit", true);
    return "admin/notice/form";
  }

  @PostMapping
  public String create(
      @RequestParam String title,
      @RequestParam String content,
      @RequestParam(required = false) String category,
      @RequestParam(required = false, defaultValue = "false") boolean isPinned,
      @RequestParam(required = false) String imageUrl,
      @AuthenticationPrincipal UserPrincipal principal) {
    User currentUser = requireUser(principal);
    NoticeCategory cat = parseCategory(category);
    Notice saved =
        adminNoticeService.create(
            title, content, cat, isPinned, blankToNull(imageUrl), currentUser);
    return "redirect:/admin/notices/" + saved.getId();
  }

  // ================= 편집 =================

  @GetMapping("/{id}")
  public String editForm(
      @PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal, Model model) {
    Notice notice;
    try {
      notice = adminNoticeService.findById(id);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없어요.");
    }
    User currentUser = requireUser(principal);
    List<NoticeAttachment> attachments = adminNoticeService.findAttachments(id);
    populateCommonModel(model, "notices");
    model.addAttribute("mode", "edit");
    model.addAttribute("notice", notice);
    model.addAttribute("attachments", attachments);
    model.addAttribute("categories", NoticeCategory.values());
    model.addAttribute("canEdit", adminNoticeService.canEdit(notice, currentUser));
    return "admin/notice/form";
  }

  @PostMapping("/{id}")
  public String update(
      @PathVariable Long id,
      @RequestParam String title,
      @RequestParam String content,
      @RequestParam(required = false) String category,
      @RequestParam(required = false, defaultValue = "false") boolean isPinned,
      @RequestParam(required = false) String imageUrl,
      @AuthenticationPrincipal UserPrincipal principal) {
    User currentUser = requireUser(principal);
    NoticeCategory cat = parseCategory(category);
    adminNoticeService.update(
        id, title, content, cat, isPinned, blankToNull(imageUrl), currentUser);
    return "redirect:/admin/notices/" + id;
  }

  @PostMapping("/{id}/delete")
  public String delete(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal principal) {
    User currentUser = requireUser(principal);
    adminNoticeService.delete(id, currentUser);
    return "redirect:/admin/notices";
  }

  // ================= 첨부 =================

  /** Qn-7 A: HTMX fragment 반환 → outerHTML swap. */
  @PostMapping("/{id}/attachments")
  public String uploadAttachment(
      @PathVariable Long id,
      @RequestParam("file") MultipartFile file,
      @AuthenticationPrincipal UserPrincipal principal,
      Model model)
      throws IOException {
    User currentUser = requireUser(principal);
    adminNoticeService.uploadAttachment(id, file, currentUser);
    return attachmentsFragment(id, currentUser, model);
  }

  @DeleteMapping("/{id}/attachments/{attachmentId}")
  public String deleteAttachment(
      @PathVariable Long id,
      @PathVariable Long attachmentId,
      @AuthenticationPrincipal UserPrincipal principal,
      Model model)
      throws IOException {
    User currentUser = requireUser(principal);
    adminNoticeService.deleteAttachment(id, attachmentId, currentUser);
    return attachmentsFragment(id, currentUser, model);
  }

  private String attachmentsFragment(Long id, User currentUser, Model model) {
    Notice notice = adminNoticeService.findById(id);
    List<NoticeAttachment> attachments = adminNoticeService.findAttachments(id);
    model.addAttribute("notice", notice);
    model.addAttribute("attachments", attachments);
    model.addAttribute("canEdit", adminNoticeService.canEdit(notice, currentUser));
    return "admin/notice/_attachments-fragment :: attachments";
  }

  // ================= 헬퍼 =================

  private void populateCommonModel(Model model, String currentPage) {
    model.addAttribute("centerScopeLabel", adminScope.centerScopeLabel());
    model.addAttribute("isSystemAdmin", adminScope.isSystemAdmin());
    model.addAttribute("currentPage", currentPage);
  }

  private NoticeCategory parseCategory(String raw) {
    if (raw == null || raw.isBlank()) return NoticeCategory.NOTICE;
    try {
      return NoticeCategory.valueOf(raw.toUpperCase());
    } catch (IllegalArgumentException e) {
      return NoticeCategory.NOTICE;
    }
  }

  private String blankToNull(String s) {
    return (s == null || s.isBlank()) ? null : s;
  }

  private User requireUser(UserPrincipal principal) {
    if (principal == null) {
      throw new AccessDeniedException("로그인이 필요해요.");
    }
    return userRepository
        .findById(principal.getId())
        .orElseThrow(() -> new AccessDeniedException("존재하지 않는 사용자에요."));
  }
}
