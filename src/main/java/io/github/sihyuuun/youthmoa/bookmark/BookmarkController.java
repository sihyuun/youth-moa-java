package io.github.sihyuuun.youthmoa.bookmark;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class BookmarkController {

  private final BookmarkService bookmarkService;

  /**
   * 즐겨찾기 토글. HTMX 요청을 가정하고 fragment 만 반환한다. (HTMX 가 outerHTML 로 자기 자신을 교체)
   *
   * <p>styleClass 는 카드 (card-bookmark-btn) vs 상세 (detail-action-icon) 구분을 위해 클라이언트가 hx-vals 로 전달.
   * 누락 시 카드 기본값 사용.
   */
  @PostMapping("/bookmarks/programs/{programId}/toggle")
  public String toggle(
      @PathVariable Long programId,
      @RequestParam(name = "styleClass", required = false, defaultValue = "card-bookmark-btn")
          String styleClass,
      @AuthenticationPrincipal UserDetails principal,
      Model model) {
    boolean bookmarked = bookmarkService.toggle(principal.getUsername(), programId);
    model.addAttribute("programId", programId);
    model.addAttribute("bookmarked", bookmarked);
    model.addAttribute("styleClass", styleClass);
    return "fragments/bookmark-button :: button";
  }
}
