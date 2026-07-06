package io.github.sihyuuun.youthmoa.search;

import io.github.sihyuuun.youthmoa.bookmark.BookmarkService;
import io.github.sihyuuun.youthmoa.program.ProgramService;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 통합 검색 결과 페이지 컨트롤러 — /search?q=키워드&tab=program|notice&page=N
 *
 * <p>@RequestParam(required=false) — 파라미터 없어도 200 응답(빈 상태 UI). Spring 이 GET 쿼리스트링을 파싱해 자동 바인딩.
 */
@Controller
@RequiredArgsConstructor
public class SearchController {

  private final SearchService searchService;
  private final ProgramService programService;
  private final BookmarkService bookmarkService;

  @GetMapping("/search")
  public String search(
      @RequestParam(required = false, defaultValue = "") String q,
      @RequestParam(required = false, defaultValue = "program") String tab,
      @RequestParam(required = false, defaultValue = "0") int page,
      @AuthenticationPrincipal UserDetails principal,
      Model model) {

    // tab 값 정규화 — "notice" 아니면 프로그램 탭.
    String activeTab = "notice".equals(tab) ? "notice" : "program";

    int programPage = "program".equals(activeTab) ? Math.max(page, 0) : 0;
    int noticePage = "notice".equals(activeTab) ? Math.max(page, 0) : 0;

    SearchResult result = searchService.search(q, programPage, noticePage);

    model.addAttribute("currentPage", "search");
    model.addAttribute("q", result.query());
    model.addAttribute("activeTab", activeTab);
    model.addAttribute("searchResult", result);
    model.addAttribute("programs", result.programs());
    model.addAttribute("notices", result.notices());

    // 프로그램 카드 렌더용 CardDto (N+1 회피)
    model.addAttribute(
        "cardDtos",
        result.programs().getContent().isEmpty()
            ? Collections.emptyList()
            : programService.toCardDtos(result.programs().getContent()));

    // 즐겨찾기 상태
    model.addAttribute(
        "bookmarkedIds",
        bookmarkService.getBookmarkedProgramIds(
            principal != null ? principal.getUsername() : null));

    // 빈 상태 추천 키워드 (D4b 에서 최근 검색어로 교체 예정)
    List<String> suggestedKeywords = List.of("취업역량", "창업지원", "마음건강", "디지털", "청년센터");
    model.addAttribute("suggestedKeywords", suggestedKeywords);

    return "search/result";
  }
}
