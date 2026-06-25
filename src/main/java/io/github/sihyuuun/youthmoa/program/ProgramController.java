package io.github.sihyuuun.youthmoa.program;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProgramController {

    private final ProgramService programService;

    @GetMapping("/programs")
    public String list(
            @RequestParam(required = false, defaultValue = "") String status,
            @RequestParam(required = false, defaultValue = "") String region,
            @RequestParam(required = false, defaultValue = "") String category,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false, defaultValue = "0") int page,
            Model model) {

        Page<Program> programs = programService.search(status, region, category, sort, page);

        model.addAttribute("currentPage", "programs");
        model.addAttribute("programs", programs);
        model.addAttribute("regions", programService.getRegions());

        model.addAttribute("filterStatus", status);
        model.addAttribute("filterRegion", region);
        model.addAttribute("filterCategory", category);
        model.addAttribute("filterSort", sort);

        return "program/list";
    }

    @GetMapping("/programs/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Program program = programService.findById(id);
        model.addAttribute("currentPage", "programs");
        model.addAttribute("program", program);
        return "program/detail";
    }
}
