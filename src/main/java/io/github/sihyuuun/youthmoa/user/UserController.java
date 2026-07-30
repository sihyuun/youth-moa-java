package io.github.sihyuuun.youthmoa.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;
  private final UserRepository userRepository;
  private final TermRepository termRepository;
  private final SecurityContextRepository securityContextRepository;

  @Value("${youthmoa.coolsms.session-valid-minutes:30}")
  private long sessionValidMinutes;

  /** 회원가입 — 아이디 중복확인 API. signup.html 의 [중복확인] 버튼에서 fetch 호출. */
  @GetMapping("/api/users/check-email")
  @ResponseBody
  public Map<String, Boolean> checkEmail(@RequestParam String email) {
    boolean available = email != null && !email.isBlank() && !userRepository.existsByEmail(email);
    return Map.of("available", available);
  }

  @GetMapping("/login")
  public String loginPage(
      @RequestParam(required = false) String error,
      @RequestParam(required = false) String logout,
      @RequestParam(required = false) String withdraw,
      jakarta.servlet.http.HttpSession session,
      Model model) {
    if (error != null) {
      model.addAttribute("errorMsg", "이메일 또는 비밀번호가 올바르지 않습니다.");
      // 실패 시 SecurityConfig 의 failureHandler 가 session 에 넣은 username 을 1회만 사용
      Object saved = session.getAttribute("savedUsername");
      if (saved != null) {
        model.addAttribute("savedUsername", saved);
        session.removeAttribute("savedUsername");
      }
    }
    if (logout != null) {
      model.addAttribute("logoutMsg", "로그아웃되었습니다.");
    }
    if (withdraw != null) {
      model.addAttribute("logoutMsg", "회원 탈퇴가 완료되었습니다.");
    }
    return "user/login";
  }

  @GetMapping("/signup")
  public String signUpPage(Model model) {
    model.addAttribute("signUpRequest", new SignUpRequest());
    // F-signup-terms-agreement: 활성 약관 목록을 폼에 동적 렌더
    model.addAttribute("activeTerms", termRepository.findByIsActiveTrueOrderBySortOrderAsc());
    return "user/signup";
  }

  @PostMapping("/signup")
  public String signUp(
      @Validated(SignUpRequest.OrderedChecks.class) @ModelAttribute SignUpRequest signUpRequest,
      BindingResult bindingResult,
      HttpServletRequest request,
      HttpServletResponse response,
      HttpSession session,
      Model model) {
    // F-signup-terms-agreement (2026-07-30 UX 결정): 약관 미동의 헬프는 GroupSequence 우회하여 항상 노출한다.
    // 다른 필수 필드 미입력 상태에서 약관 체크박스가 무반응인 것이 어색하다는 사용자 지적 반영.
    if (!userService.findMissingRequiredTermCodes(signUpRequest.getAgreements()).isEmpty()) {
      bindingResult.rejectValue("agreements", "terms.required", "이용약관과 개인정보처리방침에 모두 동의해주세요.");
    }
    if (bindingResult.hasErrors()) {
      // password 의 @Size + @Pattern 위반을 한 문장으로 통합 → model 의 passwordPolicyMsg.
      String unified =
          buildUnifiedPasswordPolicyMessage(signUpRequest.getPassword(), bindingResult);
      if (unified != null) {
        model.addAttribute("passwordPolicyMsg", unified);
      }
      model.addAttribute("activeTerms", termRepository.findByIsActiveTrueOrderBySortOrderAsc());
      return "user/signup";
    }
    // F-signup-01: 세션 재확인. hidden field 는 신뢰하지 않는다.
    // - phoneVerifiedAt 없음 → 인증 안 함
    // - 세션 만료 (sessionValidMinutes 초과) → 재인증 필요
    // - 세션 번호 vs 폼 phone 불일치 → 재인증 필요
    if (!isPhoneVerifiedInSession(session, signUpRequest.getPhone())) {
      model.addAttribute("errorMsg", "휴대폰 인증을 완료해주세요.");
      model.addAttribute("activeTerms", termRepository.findByIsActiveTrueOrderBySortOrderAsc());
      return "user/signup";
    }
    try {
      userService.signUp(signUpRequest, true);
    } catch (TermsAgreementException e) {
      // 컨트롤러 사전 검증에서 잡히지 않은 경우 방어. rejectValue → 폼 재렌더.
      bindingResult.rejectValue("agreements", "terms.required", "이용약관과 개인정보처리방침에 모두 동의해주세요.");
      model.addAttribute("activeTerms", termRepository.findByIsActiveTrueOrderBySortOrderAsc());
      return "user/signup";
    } catch (IllegalArgumentException e) {
      model.addAttribute("errorMsg", e.getMessage());
      model.addAttribute("activeTerms", termRepository.findByIsActiveTrueOrderBySortOrderAsc());
      return "user/signup";
    }
    // 회원가입 성공 → 인증 세션 정보 소거 (다음 회원가입에서 재사용 방지)
    session.removeAttribute(PhoneVerificationController.SESSION_KEY_VERIFIED_AT);
    session.removeAttribute(PhoneVerificationController.SESSION_KEY_VERIFIED_NUMBER);
    // F-signup-03 (spec §A-Q6=a): 자동 로그인 후 /welcome 온보딩으로 이동.
    WelcomeController.autoLogin(
        signUpRequest.getEmail(), userService, request, response, securityContextRepository);
    return "redirect:/welcome";
  }

  /**
   * 비밀번호 정책 위반을 한 문장으로 조립. JS 의 buildMessage 와 동일 로직. - NotBlank 만 위반 (입력 자체 없음) → null 반환 → 기존
   * NotBlank 메시지가 그대로 표시 - 정책 (@Size / @Pattern) 위반 → "비밀번호는 X 합니다." 또는 "비밀번호는 X 하고, Y 합니다."
   */
  private String buildUnifiedPasswordPolicyMessage(String pw, BindingResult br) {
    if (br.getFieldErrors("password").isEmpty()) return null;
    if (pw == null || pw.isBlank()) return null; // NotBlank 단독

    boolean missingLen = pw.length() < 8;
    boolean missingEng = !pw.matches(".*[A-Za-z].*");
    boolean missingDigit = !pw.matches(".*\\d.*");
    if (!missingLen && !missingEng && !missingDigit) return null;

    List<String> parts = new ArrayList<>();
    if (missingLen) parts.add("8자 이상이어야");
    if (missingEng && missingDigit) parts.add("영문과 숫자를 모두 포함해야");
    else if (missingEng) parts.add("영문을 포함해야");
    else if (missingDigit) parts.add("숫자를 포함해야");

    if (parts.size() == 1) return "비밀번호는 " + parts.get(0) + " 합니다.";
    return "비밀번호는 " + parts.get(0) + " 하고, " + parts.get(1) + " 합니다.";
  }

  /**
   * F-signup-01: 세션 3중 검증 (Q6=30분).
   *
   * <ol>
   *   <li>세션에 phoneVerifiedAt 존재
   *   <li>지금부터 sessionValidMinutes 이내
   *   <li>세션에 저장된 인증 번호 == 폼 phone (정규화 후 비교)
   * </ol>
   */
  private boolean isPhoneVerifiedInSession(HttpSession session, String formPhone) {
    Object verifiedAtObj =
        session.getAttribute(PhoneVerificationController.SESSION_KEY_VERIFIED_AT);
    Object verifiedNumberObj =
        session.getAttribute(PhoneVerificationController.SESSION_KEY_VERIFIED_NUMBER);
    if (!(verifiedAtObj instanceof LocalDateTime verifiedAt)) return false;
    if (!(verifiedNumberObj instanceof String verifiedNumber)) return false;

    if (Duration.between(verifiedAt, LocalDateTime.now()).toMinutes() >= sessionValidMinutes) {
      return false;
    }
    String normalizedForm = PhoneVerificationService.normalize(formPhone);
    return normalizedForm.equals(verifiedNumber);
  }
}
