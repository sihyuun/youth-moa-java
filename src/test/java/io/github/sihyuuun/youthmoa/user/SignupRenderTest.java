package io.github.sihyuuun.youthmoa.user;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * F-signup-01: signup.html 렌더 회귀 방어.
 *
 * <p>2026-07-16 재디자인: 사이드 버튼 1개 (btn-send-code) + 재전송 버튼 제거, 타이머 input 내부 배치,
 * 완료 배지가 code-row 자리를 대체.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class SignupRenderTest {

  @Autowired MockMvc mockMvc;

  @Test
  void signup_페이지가_인증_UI_마크업을_렌더한다() throws Exception {
    mockMvc
        .perform(get("/signup"))
        .andExpect(status().isOk())
        // 사이드 버튼 1개 (인증요청/재요청/재인증 라벨 전환)
        .andExpect(content().string(containsString("id=\"btn-send-code\"")))
        // 재전송 버튼은 완전히 제거됨
        .andExpect(content().string(not(containsString("id=\"btn-resend-code\""))))
        // 코드 행 · 확인 버튼
        .andExpect(content().string(containsString("id=\"code-row\"")))
        .andExpect(content().string(containsString("id=\"verify-code\"")))
        .andExpect(content().string(containsString("id=\"btn-verify-code\"")))
        // 코드 input wrap + 내부 타이머
        .andExpect(content().string(containsString("signup-code-input-wrap")))
        .andExpect(content().string(containsString("id=\"code-timer\"")))
        .andExpect(content().string(containsString("signup-code-timer")))
        // 완료 배지 (hidden 초기 상태) + 문구
        .andExpect(content().string(containsString("id=\"phone-verified-badge\"")))
        .andExpect(content().string(containsString("휴대폰 인증이 완료되었어요.")))
        // 새 placeholder
        .andExpect(content().string(containsString("인증번호 6자리")))
        // 기존 유지
        .andExpect(content().string(containsString("id=\"postcodeSearchBtn\"")))
        // CSRF meta
        .andExpect(content().string(containsString("name=\"_csrf\"")))
        // JS 로드
        .andExpect(content().string(containsString("/js/signup-phone.js")));
  }
}
