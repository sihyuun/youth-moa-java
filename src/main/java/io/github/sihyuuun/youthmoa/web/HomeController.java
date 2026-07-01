package io.github.sihyuuun.youthmoa.web;

import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * 홈 (`/`) 화면.
 *
 * 섹션 구성: Hero → Quick Stats → 프로그램 (비로그인:Top4 / 로그인:맞춤추천) → 공지 → 공간.
 * F0e 이후 카테고리 그리드·HTMX Ping 데모 섹션 제거.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/")
    public String index(@AuthenticationPrincipal UserPrincipal principal, Model model) {
        model.addAttribute("currentPage", "home");

        // Hero
        model.addAttribute("heroImageUrl", homeService.getHeroImageUrl());

        // Quick Stats — 향후 List<QuickStatDef> 리팩터 여지 (admin 관리)
        model.addAttribute("activeProgramCount", homeService.countActivePrograms());
        model.addAttribute("centerCount", homeService.countCenters());
        model.addAttribute("totalApplicantCount", homeService.countTotalApplicants());

        // 프로그램: 비로그인 = topPrograms / 로그인 = recommendedPrograms
        if (principal != null) {
            model.addAttribute("recommendedPrograms", homeService.findRecommendedPrograms(principal.getId()));
            model.addAttribute("topPrograms", List.of());
            model.addAttribute("userDisplayName", principal.getDisplayName());
        } else {
            model.addAttribute("topPrograms", homeService.findTopPrograms());
            model.addAttribute("recommendedPrograms", List.of());
        }

        // 공지
        model.addAttribute("mainNotice", homeService.findMainNotice());
        model.addAttribute("subNotices", homeService.findSubNotices());

        // 공간
        model.addAttribute("spaceImages", homeService.findSpaceImages());
        model.addAttribute("spaceLabels", List.of("상상대로", "내일스퀘어", "비행지구"));

        return "index";
    }
}
