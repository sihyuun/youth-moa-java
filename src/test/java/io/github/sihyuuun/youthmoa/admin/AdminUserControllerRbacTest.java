package io.github.sihyuuun.youthmoa.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * A5 admin-users (2026-09-15): AdminUserController RBAC 검증.
 *
 * <p>클래스 레벨 {@code @PreAuthorize("hasRole('SYSTEM_ADMIN')")} 이 실제 필터체인·method security 를 거쳐
 * CENTER_ADMIN 진입을 403 으로 차단하는지 실증. spec Qn-A 이월 결정 (SYSTEM_ADMIN 만 진입) 의 회귀 방어.
 *
 * <p>{@code @WebMvcTest} 대신 {@code @SpringBootTest} 를 사용하는 이유: SecurityConfig 의 별도 admin
 * filterChain (AdminScope · UserDetailsService · @PreAuthorize enable) 을 통째로 검증해야 하기 때문. {@code
 * AdminSecurityTest} 와 동일한 패턴.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminUserControllerRbacTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  // ================= GET /admin/users =================

  @Test
  void GET_users_anonymous_redirects_to_admin_login() throws Exception {
    mockMvc.perform(get("/admin/users")).andExpect(status().is3xxRedirection());
  }

  @Test
  void GET_users_USER_role_forbidden() throws Exception {
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    mockMvc
        .perform(get("/admin/users").with(user(new UserPrincipal(seed))))
        .andExpect(status().isForbidden());
  }

  @Test
  void GET_users_CENTER_ADMIN_forbidden() throws Exception {
    // Qn-A 이월 결정: /admin/users 는 SYSTEM_ADMIN 만 진입 가능.
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    mockMvc
        .perform(get("/admin/users").with(user(new UserPrincipal(center1))))
        .andExpect(status().isForbidden());
  }

  @Test
  void GET_users_SYSTEM_ADMIN_ok() throws Exception {
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    mockMvc
        .perform(get("/admin/users").with(user(new UserPrincipal(sysadmin))))
        .andExpect(status().isOk());
  }

  // ================= GET /admin/users/{id} =================

  @Test
  void GET_user_detail_CENTER_ADMIN_forbidden() throws Exception {
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    mockMvc
        .perform(get("/admin/users/" + seed.getId()).with(user(new UserPrincipal(center1))))
        .andExpect(status().isForbidden());
  }

  @Test
  void GET_user_detail_SYSTEM_ADMIN_ok() throws Exception {
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    mockMvc
        .perform(get("/admin/users/" + seed.getId()).with(user(new UserPrincipal(sysadmin))))
        .andExpect(status().isOk());
  }

  @Test
  void GET_user_detail_SYSTEM_ADMIN_missing_404() throws Exception {
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    mockMvc
        .perform(get("/admin/users/999999").with(user(new UserPrincipal(sysadmin))))
        .andExpect(status().isNotFound());
  }

  // ================= POST 액션 (deactivate/reactivate/role/admin-note) — RBAC =================

  @Test
  void POST_deactivate_CENTER_ADMIN_forbidden() throws Exception {
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    mockMvc
        .perform(
            post("/admin/users/" + seed.getId() + "/deactivate")
                .param("reason", "test")
                .with(user(new UserPrincipal(center1)))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void POST_role_CENTER_ADMIN_forbidden() throws Exception {
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    mockMvc
        .perform(
            post("/admin/users/" + seed.getId() + "/role")
                .param("role", "CENTER_ADMIN")
                .with(user(new UserPrincipal(center1)))
                .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void POST_deactivate_SYSTEM_ADMIN_302_redirect() throws Exception {
    // 로그인된 SYSTEM_ADMIN 이 CSRF 통과 후 POST 요청 → 302 (redirect back)
    // 통과 여부만 확인, 실제 상태 변경은 AdminUserServiceTest 가 커버.
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    mockMvc
        .perform(
            post("/admin/users/" + seed.getId() + "/deactivate")
                .param("reason", "rbac-test-reason")
                .with(user(new UserPrincipal(sysadmin)))
                .with(csrf()))
        .andExpect(status().is3xxRedirection());
  }

  @Test
  void POST_deactivate_no_csrf_forbidden() throws Exception {
    // CSRF 방어 검증: 토큰 없이 POST → 403
    User sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    User seed = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    mockMvc
        .perform(
            post("/admin/users/" + seed.getId() + "/deactivate")
                .param("reason", "test")
                .with(user(new UserPrincipal(sysadmin))))
        .andExpect(status().isForbidden());
  }

  @Test
  void formLogin_admin_users_via_login_page_flow() throws Exception {
    // 관리자 로그인 폼을 통과해 /admin/users 접근이 가능한지 최종 회귀 확인.
    // sysadmin 시드 비밀번호는 application-e2e.properties 또는 DataInitializer default 사용.
    mockMvc
        .perform(formLogin("/admin/login").user("sysadmin@youth-moa.test").password("Admin!234"))
        .andExpect(status().is3xxRedirection());
  }
}
