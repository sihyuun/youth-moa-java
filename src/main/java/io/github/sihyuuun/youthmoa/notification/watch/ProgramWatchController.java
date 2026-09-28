package io.github.sihyuuun.youthmoa.notification.watch;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A7-watcher-ui (2026-09-28): admin watch 토글 HTMX 엔드포인트.
 *
 * <p>fragment {@code admin/fragments/watch-button :: button} 를 반환하여 HTMX 가 자기 자신을 outerHTML 로 교체한다.
 * styleClass 는 목록 row vs 편집 폼 헤더 구분을 위해 클라이언트가 {@code hx-vals} 로 전달.
 *
 * <p>Bookmark 컨트롤러 패턴과 동일 (styleClass 왕복 회귀 방지 · F0h-c2 사고 회고 반영). 차이점: {@link PreAuthorize} 로
 * CENTER_ADMIN + SYSTEM_ADMIN 만 허용. USER 는 403.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CENTER_ADMIN','SYSTEM_ADMIN')")
public class ProgramWatchController {

  private final ProgramWatchService programWatchService;

  @PostMapping("/admin/programs/{programId}/watch/toggle")
  public String toggle(
      @PathVariable Long programId,
      @RequestParam(name = "styleClass", required = false, defaultValue = "list-watch-btn")
          String styleClass,
      @AuthenticationPrincipal UserDetails principal,
      Model model) {
    boolean watched = programWatchService.toggle(principal.getUsername(), programId);
    model.addAttribute("programId", programId);
    model.addAttribute("watched", watched);
    model.addAttribute("styleClass", styleClass);
    return "admin/fragments/watch-button :: button";
  }
}
