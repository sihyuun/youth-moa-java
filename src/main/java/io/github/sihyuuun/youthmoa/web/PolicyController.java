package io.github.sihyuuun.youthmoa.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 이용약관·개인정보처리방침 등 정적 정책 페이지.
 *
 * <p>회원가입 시 alert 임시 처리를 실 페이지 링크로 대체 (2026-07-28).
 */
@Controller
public class PolicyController {

  @GetMapping("/terms")
  public String terms() {
    return "policy/terms";
  }

  @GetMapping("/privacy")
  public String privacy() {
    return "policy/privacy";
  }

  /** 260819: 이메일 무단 수집거부 페이지 (HANDOFF.md L646~L652). */
  @GetMapping("/email-policy")
  public String emailPolicy() {
    return "policy/email-policy";
  }
}
