package io.github.sihyuuun.youthmoa.bookmark;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class BookmarkController {

  private final BookmarkService bookmarkService;

  /** 즐겨찾기 토글. HTMX 요청을 가정하고 fragment 만 반환한다. (HTMX 가 outerHTML 로 자기 자신을 교체) */
  @PostMapping("/bookmarks/programs/{programId}/toggle")
  public String toggle(
      @PathVariable Long programId, @AuthenticationPrincipal UserDetails principal, Model model) {
    boolean bookmarked = bookmarkService.toggle(principal.getUsername(), programId);
    model.addAttribute("programId", programId);
    model.addAttribute("bookmarked", bookmarked);
    return "fragments/bookmark-button :: button";
  }
}
