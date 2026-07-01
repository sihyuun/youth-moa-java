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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;
    private final BookmarkService bookmarkService;
    private final ApplicationRepository applicationRepository;

    @GetMapping("/programs")
    public String list(
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(name = "regions", required = false) List<String> regions,
            @RequestParam(name = "centers", required = false) List<String> centers,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestHeader(name = "HX-Request", required = false) String hxRequest,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        List<String> safeRegions = regions == null ? Collections.emptyList() : regions;
        List<String> safeCenters = centers == null ? Collections.emptyList() : centers;

        Page<Program> programs = programService.search(status, safeRegions, safeCenters, sort, page);

        model.addAttribute("currentPage", "programs");
        model.addAttribute("programs", programs);

        // 사이드바 (featured 5) + 팝오버 (전체)
        model.addAttribute("sidebarRegions", programService.getSidebarRegions());
        model.addAttribute("sidebarCenters", programService.getSidebarCenters());
        model.addAttribute("allRegions", programService.getAllRegions());
        model.addAttribute("allCenters", programService.getAllCenters());

        model.addAttribute("filterStatus", status);
        model.addAttribute("filterRegions", safeRegions);
        model.addAttribute("filterCenters", safeCenters);
        model.addAttribute("filterSort", sort);

        // 활성 칩 — key=group:value, label, removeQuery
        List<Map<String, String>> activeFilters = buildActiveFilters(status, safeRegions, safeCenters, sort);
        model.addAttribute("activeFilters", activeFilters);

        // 인증된 사용자의 즐겨찾기 program id Set (카드 N개 N+1 회피)
        model.addAttribute("bookmarkedIds",
                bookmarkService.getBookmarkedProgramIds(
                        principal != null ? principal.getUsername() : null));

        // htmx 부분 갱신
        if (hxRequest != null && !hxRequest.isBlank()) {
            return "program/_list-fragment :: list-region";
        }
        return "program/list";
    }

    private List<Map<String, String>> buildActiveFilters(String status, List<String> regions,
                                                         List<String> centers, String sort) {
        List<Map<String, String>> chips = new ArrayList<>();
        for (String r : regions) {
            Map<String, String> chip = new LinkedHashMap<>();
            chip.put("group", "regions");
            chip.put("value", r);
            chip.put("label", r);
            chips.add(chip);
        }
        for (String c : centers) {
            Map<String, String> chip = new LinkedHashMap<>();
            chip.put("group", "centers");
            chip.put("value", c);
            chip.put("label", c);
            chips.add(chip);
        }
        return chips;
    }

    @GetMapping("/programs/{id}")
    public String detail(@PathVariable Long id,
                         @AuthenticationPrincipal UserDetails principal,
                         Model model) {
        Program program = programService.findById(id);
        boolean bookmarked = principal != null
                && bookmarkService.isBookmarked(principal.getUsername(), id);

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
