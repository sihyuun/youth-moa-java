package io.github.sihyuuun.youthmoa.web;

import io.github.sihyuuun.youthmoa.bookmark.BookmarkService;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 홈 (`/`) 화면.
 *
 * <p>섹션 구성: Hero → Quick Stats → 프로그램 (비로그인:Top4 / 로그인:맞춤추천) → 공지 → 공간. F0e 이후 카테고리 그리드·HTMX Ping
 * 데모 섹션 제거.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

  private final HomeService homeService;
  private final BookmarkService bookmarkService;

  @GetMapping("/")
  public String index(@AuthenticationPrincipal UserPrincipal principal, Model model) {
    model.addAttribute("currentPage", "home");

    // Hero (F0e-2: 6장 로테이션. 1장이어도 리스트로 렌더)
    model.addAttribute("heroImageUrls", homeService.getHeroImageUrls());

    // Quick Stats — 향후 List<QuickStatDef> 리팩터 여지 (admin 관리)
    model.addAttribute("activeProgramCount", homeService.countActivePrograms());
    model.addAttribute("centerCount", homeService.countCenters());
    model.addAttribute("totalApplicantCount", homeService.countTotalApplicants());

    // 프로그램: 비로그인 = topPrograms / 로그인 = recommendedPrograms (ProgramCardDto — CapacityBar 포함)
    if (principal != null) {
      model.addAttribute(
          "recommendedPrograms", homeService.findRecommendedProgramCards(principal.getId()));
      model.addAttribute("topPrograms", List.of());
      model.addAttribute("userDisplayName", principal.getDisplayName());
      // 맞춤 추천 제목 옆 관심 태그 chip — proto L581. 지역·카테고리 둘 다 없으면 null.
      model.addAttribute(
          "recommendInterestChip", homeService.getRecommendInterestChip(principal.getId()));
    } else {
      model.addAttribute("topPrograms", homeService.findTopProgramCards());
      model.addAttribute("recommendedPrograms", List.of());
    }

    // 홈 카드 즐겨찾기 별 표시용. 비인증은 빈 Set → fragment 가 /login 링크 렌더.
    model.addAttribute(
        "bookmarkedIds",
        principal != null
            ? bookmarkService.getBookmarkedProgramIds(principal.getUsername())
            : Collections.emptySet());

    // 공지
    model.addAttribute("mainNotice", homeService.findMainNotice());
    model.addAttribute("subNotices", homeService.findSubNotices());

    // 공간
    model.addAttribute("spaceImages", homeService.findSpaceImages());
    model.addAttribute("spaceLabels", List.of("상상대로", "내일스퀘어", "비행지구"));

    return "index";
  }
}
