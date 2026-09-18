package io.github.sihyuuun.youthmoa.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * A5-1 admin-staff-management (2026-09-18): {@link PasswordChangeRequiredInterceptor} 가 강제 이동시키는
 * 비밀번호 변경 화면. 관리자가 발급한 계정 · 관리자 재발급 리셋 후 최초 로그인 시 진입한다.
 *
 * <p>Qn-11 A: 강제 변경 flow 에서는 이전 password 검증을 유지한다 (임시 password 를 본인이 알고 있다는 확인). 새 password 는 사용자
 * password 정책 (8자 이상 + 영문/숫자 포함) 을 재활용한다.
 *
 * <p>flag=FALSE 인 유저도 이 화면을 열 수 있다 — 마이페이지에서 `/password/change` 로 진입하는 케이스는 후속 자율 변경으로 확장 가능. 현재는
 * 강제 변경 케이스만 실무 요구.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/password/change")
public class PasswordChangeController {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @GetMapping
  public String form(@AuthenticationPrincipal UserPrincipal principal, Model model) {
    if (principal == null) {
      return "redirect:/login";
    }
    model.addAttribute("email", principal.getUsername());
    model.addAttribute("mustChangePassword", principal.isMustChangePassword());
    return "user/password-change";
  }

  @PostMapping
  @Transactional
  public String submit(
      @RequestParam String currentPassword,
      @RequestParam String newPassword,
      @RequestParam String newPasswordConfirm,
      @AuthenticationPrincipal UserPrincipal principal,
      RedirectAttributes ra) {
    if (principal == null) {
      return "redirect:/login";
    }
    if (currentPassword == null || currentPassword.isBlank()) {
      ra.addFlashAttribute("flashError", "현재 비밀번호를 입력해주세요.");
      return "redirect:/password/change";
    }
    if (newPassword == null || newPassword.length() < 8) {
      ra.addFlashAttribute("flashError", "새 비밀번호는 8자 이상이어야 합니다.");
      return "redirect:/password/change";
    }
    if (!newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*")) {
      ra.addFlashAttribute("flashError", "새 비밀번호는 영문과 숫자를 모두 포함해야 합니다.");
      return "redirect:/password/change";
    }
    if (!newPassword.equals(newPasswordConfirm)) {
      ra.addFlashAttribute("flashError", "새 비밀번호와 확인이 일치하지 않아요.");
      return "redirect:/password/change";
    }
    User user =
        userRepository
            .findById(principal.getId())
            .orElseThrow(() -> new IllegalStateException("사용자 정보를 확인할 수 없어요."));
    if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
      ra.addFlashAttribute("flashError", "현재 비밀번호가 일치하지 않아요.");
      return "redirect:/password/change";
    }
    if (passwordEncoder.matches(newPassword, user.getPassword())) {
      ra.addFlashAttribute("flashError", "새 비밀번호는 현재 비밀번호와 달라야 합니다.");
      return "redirect:/password/change";
    }
    user.changePassword(passwordEncoder.encode(newPassword));
    // mustChangePassword=FALSE + passwordChangedAt=now 는 changePassword() 도메인 메서드에서 처리
    ra.addFlashAttribute("flashMessage", "비밀번호를 변경했어요. 다시 로그인해주세요.");
    return "redirect:/logout";
  }
}
