package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.notification.NotificationChannel;
import io.github.sihyuuun.youthmoa.notification.NotificationChannelResolver;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramService;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final ApplicationRepository applicationRepository;
    private final ProgramService programService;
    private final UserRepository userRepository;
    private final NotificationChannelResolver notificationChannelResolver;

    @GetMapping("/programs/{id}/apply")
    public String applyForm(@PathVariable("id") Long programId,
                            @AuthenticationPrincipal UserDetails principal,
                            Model model) {
        addCommonModel(programId, principal, model);
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
            addCommonModel(programId, principal, model);
            return "application/apply";
        }

        Application saved;
        try {
            saved = applicationService.apply(principal.getUsername(), programId, applyRequest);
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("applyError", e.getMessage());
            return "redirect:/programs/" + programId + "/apply";
        }

        // D1b: 신청 완료 페이지로 이동 (쿼리파라미터 방식, 새로고침 안전)
        return "redirect:/apply/complete?applicationId=" + saved.getId();
    }

    /**
     * D1b: 신청 완료 페이지.
     * <p>
     * 권한 위반 (다른 사용자의 신청 ID) 시 <b>404</b> 를 반환한다.
     * 403 을 쓰지 않는 이유: 존재 여부 자체를 노출하지 않기 위함.
     */
    @GetMapping("/apply/complete")
    @Transactional(readOnly = true)
    public String complete(@RequestParam("applicationId") Long applicationId,
                           @AuthenticationPrincipal UserDetails principal,
                           Model model) {
        User currentUser = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        // OSIV=false 환경 대응: program·user 를 fetch join 으로 미리 로드 (템플릿 lazy 접근 방지).
        Application application = applicationRepository.findWithProgramAndUserById(applicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (!application.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        List<NotificationChannel> channels = notificationChannelResolver.activeChannelsFor(currentUser);

        // "application" 은 Thymeleaf 에서 ServletContext scope 예약어와 충돌 → myApplication 사용
        model.addAttribute("myApplication", application);
        model.addAttribute("program", application.getProgram());
        model.addAttribute("channels", channels);
        model.addAttribute("channelSubtitle", buildChannelSubtitle(channels));
        return "application/complete";
    }

    /**
     * 활성 채널 수에 따라 신청 완료 페이지 부제 문구를 조립.
     * <ul>
     *   <li>3: "결과는 카카오톡·문자·이메일로 안내드려요"</li>
     *   <li>2: "결과는 {A}·{B}로 안내드려요" (enum 순서: KAKAO → SMS → EMAIL)</li>
     *   <li>1: "결과는 {A}로 안내드려요"</li>
     *   <li>0: fallback — "결과는 마이페이지 &gt; 신청 현황에서 확인해주세요" (UI 상 발생 불가)</li>
     * </ul>
     */
    private String buildChannelSubtitle(List<NotificationChannel> channels) {
        if (channels.isEmpty()) {
            return "결과는 마이페이지 > 신청 현황에서 확인해주세요";
        }
        StringBuilder sb = new StringBuilder("결과는 ");
        for (int i = 0; i < channels.size(); i++) {
            if (i > 0) sb.append("·");
            sb.append(channels.get(i).getLabel());
        }
        sb.append("로 안내드려요");
        return sb.toString();
    }

    /** 폼 렌더에 필요한 program + currentUser 모델 채움 (prototype.tsx 의 신청자 정보 readonly 섹션용). */
    private void addCommonModel(Long programId, UserDetails principal, Model model) {
        Program program = programService.findById(programId);
        User currentUser = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        model.addAttribute("currentPage", "programs");
        model.addAttribute("program", program);
        model.addAttribute("currentUser", currentUser);
    }
}
