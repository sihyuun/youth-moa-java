package io.github.sihyuuun.youthmoa.common.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * P0-2 회귀 방어: CSRF 활성 상태 검증.
 *
 * <p>Spring Security 7 기본 CSRF 는 세션 저장. POST 요청은 반드시 X-CSRF-TOKEN 헤더 또는 폼 파라미터 필요.
 *
 * <ul>
 *   <li>POST /logout without CSRF → 403
 *   <li>POST /logout with CSRF → 302 (로그아웃 후 /login?logout)
 *   <li>POST /signup without CSRF → 403
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class SecurityConfigCsrfTest {

  @Autowired MockMvc mockMvc;

  @Test
  void POST_logout_without_CSRF_403() throws Exception {
    mockMvc
        .perform(post("/logout").with(user("seed1@youth-moa.test")))
        .andExpect(status().isForbidden());
  }

  @Test
  void POST_logout_with_CSRF_302() throws Exception {
    mockMvc
        .perform(post("/logout").with(user("seed1@youth-moa.test")).with(csrf()))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void POST_signup_without_CSRF_403() throws Exception {
    mockMvc.perform(post("/signup")).andExpect(status().isForbidden());
  }
}
