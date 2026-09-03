package io.github.sihyuuun.youthmoa.admin;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A1: 관리자 트랙 RBAC 슬라이스 회귀 방어 (Qn-1 A · 별도 SecurityFilterChain 확인).
 *
 * <ul>
 *   <li>비인증 /admin → 302 → /admin/login
 *   <li>USER 로그인 /admin → 403
 *   <li>CENTER_ADMIN /admin → 200
 *   <li>SYSTEM_ADMIN /admin → 200
 *   <li>USER 로그인 /admin/login GET → 200 (permitAll)
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminSecurityTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  @Test
  void 비인증_admin_접근_시_admin_login_으로_리다이렉트() throws Exception {
    mockMvc
        .perform(get("/admin"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/admin/login"));
  }

  @Test
  void USER_계정으로_admin_접근_시_403() throws Exception {
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(seed);
    mockMvc.perform(get("/admin").with(user(principal))).andExpect(status().isForbidden());
  }

  @Test
  void CENTER_ADMIN_로_admin_접근_시_200() throws Exception {
    User admin = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(admin);
    mockMvc.perform(get("/admin").with(user(principal))).andExpect(status().isOk());
  }

  @Test
  void SYSTEM_ADMIN_로_admin_접근_시_200() throws Exception {
    User admin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(admin);
    mockMvc.perform(get("/admin").with(user(principal))).andExpect(status().isOk());
  }

  @Test
  void USER_로그인_상태로_admin_login_GET_시_200() throws Exception {
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(seed);
    mockMvc.perform(get("/admin/login").with(user(principal))).andExpect(status().isOk());
  }

  @Test
  void 비인증_admin_login_GET_은_200_이며_필수_마크업_포함() throws Exception {
    mockMvc
        .perform(get("/admin/login"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist("Location"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(containsString("관리자 로그인")));
  }
}
