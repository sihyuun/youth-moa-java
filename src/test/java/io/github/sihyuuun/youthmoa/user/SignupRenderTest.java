package io.github.sihyuuun.youthmoa.user;

import static org.hamcrest.Matchers.containsString;
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
 * <p>인증 UI 마크업 (btn-send-code, code-row, verified-badge) + 기존 postcodeSearchBtn 유지 확인.
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
        .andExpect(content().string(containsString("id=\"btn-send-code\"")))
        .andExpect(content().string(containsString("id=\"code-row\"")))
        .andExpect(content().string(containsString("id=\"phone-verified-badge\"")))
        .andExpect(content().string(containsString("id=\"verify-code\"")))
        .andExpect(content().string(containsString("id=\"btn-verify-code\"")))
        .andExpect(content().string(containsString("id=\"btn-resend-code\"")))
        // 기존 유지
        .andExpect(content().string(containsString("id=\"postcodeSearchBtn\"")))
        // CSRF meta
        .andExpect(content().string(containsString("name=\"_csrf\"")))
        // JS 로드
        .andExpect(content().string(containsString("/js/signup-phone.js")));
  }
}
