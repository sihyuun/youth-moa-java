package io.github.sihyuuun.youthmoa.user;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * F0i: 아이디 / 비밀번호 찾기 컨트롤러.
 *
 * <p>Q2 결정: SMTP 미도입 → 본인 확인 후 즉시 재설정 흐름.
 *
 * <p>세션 키:
 *
 * <ul>
 *   <li>{@code fp_verifiedUserId} — 본인 확인 통과한 사용자 id
 *   <li>{@code fp_verifiedAt} — 확인 시각 (10분 만료)
 * </ul>
 */
@Controller
@RequiredArgsConstructor
public class FindAccountController {

  private static final String SESSION_VERIFIED_USER_ID = "fp_verifiedUserId";
  private static final String SESSION_VERIFIED_AT = "fp_verifiedAt";
  private static final long VERIFY_TTL_MINUTES = 10;
  private static final long MISMATCH_DELAY_MS = 200;

  private final FindAccountService findAccountService;

  // ─────────── 아이디 찾기 ───────────

  @GetMapping("/find-id")
  public String findIdPage(Model model) {
    if (!model.containsAttribute("findIdRequest")) {
      model.addAttribute("findIdRequest", new FindIdRequest());
    }
    return "user/find-id";
  }

  @PostMapping("/find-id")
  public String findId(
      @Valid @ModelAttribute FindIdRequest findIdRequest,
      BindingResult bindingResult,
      Model model) {
    if (bindingResult.hasErrors()) {
      return "user/find-id";
    }
    Optional<User> matched =
        findAccountService.findEmailByNameAndPhone(
            findIdRequest.getName(), findIdRequest.getPhone());
    if (matched.isEmpty()) {
      sleepQuietly(MISMATCH_DELAY_MS);
      model.addAttribute("errorMsg", "일치하는 계정이 없습니다. 입력하신 정보를 확인해주세요.");
      return "user/find-id";
    }
    model.addAttribute("maskedEmail", EmailMaskingUtil.mask(matched.get().getEmail()));
    return "user/find-id-result";
  }

  // ─────────── 비밀번호 찾기 (본인 확인) ───────────

  @GetMapping("/find-password")
  public String findPasswordPage(Model model) {
    if (!model.containsAttribute("findPasswordRequest")) {
      model.addAttribute("findPasswordRequest", new FindPasswordRequest());
    }
    return "user/find-password";
  }

  @PostMapping("/find-password")
  public String findPassword(
      @Valid @ModelAttribute FindPasswordRequest findPasswordRequest,
      BindingResult bindingResult,
      HttpSession session,
      Model model) {
    if (bindingResult.hasErrors()) {
      return "user/find-password";
    }
    Optional<User> matched =
        findAccountService.verifyForPasswordReset(
            findPasswordRequest.getEmail(),
            findPasswordRequest.getName(),
            findPasswordRequest.getPhone());
    if (matched.isEmpty()) {
      sleepQuietly(MISMATCH_DELAY_MS);
      model.addAttribute("errorMsg", "일치하는 계정이 없습니다. 입력하신 정보를 확인해주세요.");
      return "user/find-password";
    }
    session.setAttribute(SESSION_VERIFIED_USER_ID, matched.get().getId());
    session.setAttribute(SESSION_VERIFIED_AT, LocalDateTime.now());
    model.addAttribute("passwordResetRequest", new PasswordResetRequest());
    return "user/find-password-reset";
  }

  // ─────────── 비밀번호 재설정 ───────────

  @GetMapping("/find-password/reset")
  public String resetPage(HttpSession session, Model model) {
    if (!isVerified(session)) {
      return "redirect:/find-password";
    }
    if (!model.containsAttribute("passwordResetRequest")) {
      model.addAttribute("passwordResetRequest", new PasswordResetRequest());
    }
    return "user/find-password-reset";
  }

  @PostMapping("/find-password/reset")
  public String reset(
      @Valid @ModelAttribute PasswordResetRequest passwordResetRequest,
      BindingResult bindingResult,
      HttpSession session,
      Model model) {
    if (!isVerified(session)) {
      return "redirect:/find-password";
    }
    if (bindingResult.hasErrors()) {
      return "user/find-password-reset";
    }
    if (!passwordResetRequest.getPassword().equals(passwordResetRequest.getPasswordConfirm())) {
      bindingResult.rejectValue("passwordConfirm", "mismatch", "비밀번호가 일치하지 않습니다.");
      return "user/find-password-reset";
    }
    Long userId = (Long) session.getAttribute(SESSION_VERIFIED_USER_ID);
    findAccountService.resetPassword(userId, passwordResetRequest.getPassword());
    session.invalidate();
    return "redirect:/login?reset";
  }

  private boolean isVerified(HttpSession session) {
    Long userId = (Long) session.getAttribute(SESSION_VERIFIED_USER_ID);
    LocalDateTime at = (LocalDateTime) session.getAttribute(SESSION_VERIFIED_AT);
    if (userId == null || at == null) return false;
    if (at.plusMinutes(VERIFY_TTL_MINUTES).isBefore(LocalDateTime.now())) {
      session.removeAttribute(SESSION_VERIFIED_USER_ID);
      session.removeAttribute(SESSION_VERIFIED_AT);
      return false;
    }
    return true;
  }

  private void sleepQuietly(long ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException ignored) {
      Thread.currentThread().interrupt();
    }
  }
}
