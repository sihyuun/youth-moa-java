package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ProgramService programService;

    @GetMapping("/programs/{id}/apply")
    public String applyForm(@PathVariable("id") Long programId,
                            @AuthenticationPrincipal UserDetails principal,
                            Model model) {
        Program program = programService.findById(programId);
        model.addAttribute("currentPage", "programs");
        model.addAttribute("program", program);
        model.addAttribute("applyRequest", new ApplyRequest());
        return "application/apply";
    }

    @PostMapping("/programs/{id}/apply")
    public String apply(@PathVariable("id") Long programId,
                        @Valid @ModelAttribute ApplyRequest applyRequest,
                        BindingResult bindingResult,
                        @AuthenticationPrincipal UserDetails principal,
                        Model model,
                        RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            Program program = programService.findById(programId);
            model.addAttribute("currentPage", "programs");
            model.addAttribute("program", program);
            return "application/apply";
        }

        try {
            applicationService.apply(principal.getUsername(), programId, applyRequest);
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("applyError", e.getMessage());
            return "redirect:/programs/" + programId + "/apply";
        }

        redirectAttributes.addFlashAttribute("applySuccess", "신청이 접수되었습니다. 심사 결과는 곧 알려드릴게요.");
        return "redirect:/programs/" + programId;
    }
}
