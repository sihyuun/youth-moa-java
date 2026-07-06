package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.application.dto.CancelRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * D5 마이페이지 신청 취소 컨트롤러.
 *
 * <p>모달 폼 submit 을 처리한다. 성공 시 {@code /mypage?tab=history} 로 리다이렉트.
 */
@Controller
@RequiredArgsConstructor
public class MyPageCancelController {

  private final ApplicationService applicationService;

  @PostMapping("/mypage/applications/{id}/cancel")
  public String cancel(
      @PathVariable("id") Long applicationId,
      @Valid @ModelAttribute CancelRequest request,
      BindingResult bindingResult,
      @AuthenticationPrincipal UserDetails principal,
      RedirectAttributes redirectAttributes) {

    if (bindingResult.hasErrors()) {
      redirectAttributes.addFlashAttribute("cancelError", "취소 사유를 선택해주세요.");
      return "redirect:/mypage?tab=history";
    }

    CancelReason reason = CancelReason.fromCode(request.getReasonCode()).orElse(null);
    if (reason == null) {
      redirectAttributes.addFlashAttribute("cancelError", "취소 사유를 선택해주세요.");
      return "redirect:/mypage?tab=history";
    }

    // OTHER 인 경우 사용자 입력 필수
    if (reason == CancelReason.OTHER
        && (request.getReasonText() == null || request.getReasonText().isBlank())) {
      redirectAttributes.addFlashAttribute("cancelError", "취소 사유를 입력해주세요.");
      return "redirect:/mypage?tab=history";
    }

    String composed =
        reason == CancelReason.OTHER
            ? reason.getLabel() + ": " + request.getReasonText().trim()
            : reason.getLabel();

    try {
      applicationService.cancel(applicationId, principal.getUsername(), composed);
    } catch (IllegalStateException | IllegalArgumentException e) {
      redirectAttributes.addFlashAttribute("cancelError", e.getMessage());
      return "redirect:/mypage?tab=history";
    }

    redirectAttributes.addFlashAttribute("mypageToast", "취소되었습니다.");
    return "redirect:/mypage?tab=history";
  }
}
