package io.github.sihyuuun.youthmoa.program;

import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import io.github.sihyuuun.youthmoa.bookmark.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;
    private final BookmarkService bookmarkService;
    private final ApplicationRepository applicationRepository;

    @GetMapping("/programs")
    public String list(
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String region,
            @RequestParam(required = false, defaultValue = "") String category,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        Page<Program> programs = programService.search(status, region, category, sort, page);

        model.addAttribute("currentPage", "programs");
        model.addAttribute("programs", programs);
        model.addAttribute("regions", programService.getRegions());

        model.addAttribute("filterStatus", status);
        model.addAttribute("filterRegion", region);
        model.addAttribute("filterCategory", category);
        model.addAttribute("filterSort", sort);

        // 인증된 사용자의 즐겨찾기 program id Set (카드 N개 N+1 회피)
        model.addAttribute("bookmarkedIds",
                bookmarkService.getBookmarkedProgramIds(
                        principal != null ? principal.getUsername() : null));

        return "program/list";
    }

    @GetMapping("/programs/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails principal,
                         Model model) {
        Program program = programService.findById(id);
        boolean bookmarked = principal != null
                && bookmarkService.isBookmarked(principal.getUsername(), id);

        // prototype.tsx capInfo() — applied / capacity 비율 + 경쟁률
        long appliedCount = applicationRepository.countByProgramAndStatusIn(
                program,
                List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED)
        );

        int applicationRate = 0;
        String competitionRatio = "0.0";
        if (program.getCapacity() != null && program.getCapacity() > 0) {
            double ratio = (double) appliedCount / program.getCapacity();
            applicationRate = Math.min(100, (int) Math.round(ratio * 100));
            competitionRatio = String.format("%.1f", ratio);
        }

        model.addAttribute("currentPage", "programs");
        model.addAttribute("program", program);
        model.addAttribute("bookmarked", bookmarked);
        model.addAttribute("appliedCount", appliedCount);
        model.addAttribute("applicationRate", applicationRate);
        model.addAttribute("competitionRatio", competitionRatio);
        return "program/detail";
    }
}
