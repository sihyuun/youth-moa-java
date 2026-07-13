package io.github.sihyuuun.youthmoa.common.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * P0-2 회귀 방어: {@code /admin/**} 접근 통제.
 *
 * <ul>
 *   <li>익명 사용자 → 302 로그인 리다이렉트
 *   <li>ROLE_USER → 403 Forbidden
 *   <li>ROLE_CENTER_ADMIN → 인가 통과 (컨트롤러 부재 시 404, 302 로그인 리다이렉트는 아님)
 *   <li>ROLE_SYSTEM_ADMIN → 인가 통과
 * </ul>
 *
 * A1 (`/admin/login` 페이지·컨트롤러) 는 이월 상태라 실제 URL 은 404 이지만, security matcher 는 통과해야 함.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class SecurityConfigAdminAccessTest {

  @Autowired MockMvc mockMvc;

  @Test
  void 익명_admin_경로_로그인_리다이렉트() throws Exception {
    mockMvc.perform(get("/admin/dashboard")).andExpect(status().is3xxRedirection());
  }

  @Test
  void ROLE_USER_admin_경로_403() throws Exception {
    mockMvc
        .perform(get("/admin/dashboard").with(user("u@test").roles("USER")))
        .andExpect(status().isForbidden());
  }

  @Test
  void ROLE_CENTER_ADMIN_admin_경로_인가_통과() throws Exception {
    // 인가 통과 시 컨트롤러 없어서 404 (302 로그인 리다이렉트 아님).
    mockMvc
        .perform(get("/admin/dashboard").with(user("ca@test").roles("CENTER_ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void ROLE_SYSTEM_ADMIN_admin_경로_인가_통과() throws Exception {
    mockMvc
        .perform(get("/admin/dashboard").with(user("sa@test").roles("SYSTEM_ADMIN")))
        .andExpect(status().isNotFound());
  }
}
