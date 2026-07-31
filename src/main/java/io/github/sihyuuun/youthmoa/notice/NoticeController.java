package io.github.sihyuuun.youthmoa.notice;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequiredArgsConstructor
public class NoticeController {

  private final NoticeService noticeService;

  /**
   * 공지 목록.
   *
   * <p>HTMX 부분 갱신: 카테고리 탭 · 페이지네이션 클릭 시 HX-Request 헤더가 실리면 목록 fragment 만 반환하여 outerHTML swap.
   */
  @GetMapping("/notices")
  public String list(
      @RequestParam(required = false) String category,
      @RequestParam(required = false, defaultValue = "0") int page,
      @RequestHeader(name = "HX-Request", required = false) String hxRequest,
      Model model) {

    NoticeCategory cat = parseCategory(category);
    Page<Notice> notices = noticeService.list(cat, page);

    model.addAttribute("currentPage", "notices");
    model.addAttribute("notices", notices);
    model.addAttribute("filterCategory", cat);
    model.addAttribute("categories", NoticeCategory.values());

    // 페이지 그룹 (5개씩) 계산 — prototype 은 3개 예시지만 명세는 5.
    int totalPages = Math.max(1, notices.getTotalPages());
    int current = notices.getNumber(); // 0-based
    int groupSize = NoticeService.PAGE_GROUP_SIZE;
    int groupStart = (current / groupSize) * groupSize;
    int groupEnd = Math.min(totalPages - 1, groupStart + groupSize - 1);
    model.addAttribute("pageGroupStart", groupStart);
    model.addAttribute("pageGroupEnd", groupEnd);
    model.addAttribute("prevGroupPage", Math.max(0, groupStart - 1));
    model.addAttribute("nextGroupPage", Math.min(totalPages - 1, groupEnd + 1));
    model.addAttribute("hasPrevGroup", groupStart > 0);
    model.addAttribute("hasNextGroup", groupEnd < totalPages - 1);

    if (hxRequest != null && !hxRequest.isBlank()) {
      // 탭 + 목록 wrapper 를 함께 재렌더 (탭 active 클래스도 갱신) — 2026-07-06 UX fix
      return "notice/_list-fragment :: content-region";
    }
    return "notice/list";
  }

  /**
   * 공지 상세. @Lob content 접근 및 viewCount++ 를 위해 Service 에서 @Transactional 처리. 이전/다음글은 전체 기준 (카테고리 필터
   * 무시, 명세 Q3-a).
   */
  @GetMapping("/notices/{id}")
  public String detail(@PathVariable Long id, Model model) {
    Notice notice;
    try {
      notice = noticeService.detailAndIncreaseView(id);
    } catch (IllegalArgumentException e) {
      // ApplicationController 패턴과 동일 — 404 로 매핑해 실 사용자 경험 정합
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다.");
    }
    Optional<Notice> prev = noticeService.findPrev(id);
    Optional<Notice> next = noticeService.findNext(id);

    model.addAttribute("currentPage", "notices");
    // "notice" 이름은 예약어가 아니지만 안전하게 짧은 별칭 유지. 단, application 사고 회피 규칙 참고.
    model.addAttribute("notice", notice);
    model.addAttribute("prevNotice", prev.orElse(null));
    model.addAttribute("nextNotice", next.orElse(null));
    model.addAttribute("attachments", noticeService.findAttachments(id));
    return "notice/detail";
  }

  /**
   * F-notice-attachment: 공지사항 첨부파일 다운로드. wireframe WF-6-002 하단 파일명 클릭 시 진입.
   *
   * <p>경로에 {noticeId}·{attachmentId} 를 함께 두어 서비스가 소속 관계를 검증한다 (URL 조작 방어). 파일명은 RFC 5987 filename*
   * 로 UTF-8 인코딩. Content-Type 은 저장된 값 사용, 없으면 application/octet-stream 로 fallback.
   */
  @GetMapping("/notices/{noticeId}/attachments/{attachmentId}/download")
  public ResponseEntity<ByteArrayResource> downloadAttachment(
      @PathVariable Long noticeId, @PathVariable Long attachmentId) {
    NoticeAttachment attachment;
    try {
      attachment = noticeService.findAttachmentForDownload(noticeId, attachmentId);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }

    String encoded =
        URLEncoder.encode(attachment.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
    String disposition = "attachment; filename*=UTF-8''" + encoded;

    MediaType mediaType =
        attachment.getContentType() != null && !attachment.getContentType().isBlank()
            ? MediaType.parseMediaType(attachment.getContentType())
            : MediaType.APPLICATION_OCTET_STREAM;

    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
        .contentType(mediaType)
        .contentLength(attachment.getData().length)
        .body(new ByteArrayResource(attachment.getData()));
  }

  private NoticeCategory parseCategory(String raw) {
    if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw)) return null;
    try {
      return NoticeCategory.valueOf(raw.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
