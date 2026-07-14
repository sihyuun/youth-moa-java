package io.github.sihyuuun.youthmoa.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
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
  private final SecurityContextRepository securityContextRepository;

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
    return "user/login";
  }

  @GetMapping("/signup")
  public String signUpPage(Model model) {
    model.addAttribute("signUpRequest", new SignUpRequest());
    return "user/signup";
  }

  @PostMapping("/signup")
  public String signUp(
      @Validated(SignUpRequest.OrderedChecks.class) @ModelAttribute SignUpRequest signUpRequest,
      BindingResult bindingResult,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {
    if (bindingResult.hasErrors()) {
      // password 의 @Size + @Pattern 위반을 한 문장으로 통합 → model 의 passwordPolicyMsg.
      String unified =
          buildUnifiedPasswordPolicyMessage(signUpRequest.getPassword(), bindingResult);
      if (unified != null) {
        model.addAttribute("passwordPolicyMsg", unified);
      }
      return "user/signup";
    }
    try {
      userService.signUp(signUpRequest);
    } catch (IllegalArgumentException e) {
      model.addAttribute("errorMsg", e.getMessage());
      return "user/signup";
    }
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
}
