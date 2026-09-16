package io.github.sihyuuun.youthmoa.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
 * A5 admin-users (2026-09-15): {@code /admin/users} 목록 Thymeleaf 실 렌더 회귀 방어.
 *
 * <p>compileJava + JpaMappingTest 만으로는 Thymeleaf 파싱·SpEL·fragment 오염을 감지 못 함 (F0h-c2 회고). prototype
 * L1166~1272 과 실제 렌더 마크업의 격차 감시.
 *
 * <p>커버:
 *
 * <ul>
 *   <li>GNB "users" 활성
 *   <li>액션바 (검색 인풋 · 4 role filter 탭)
 *   <li>테이블 9컬럼 헤더 (No · 이름 · 이메일 · 성별 · 권한 · 핸드폰 · 가입일 · 최근접속 · 상태) — Qn-9 이월로 checkbox 제거됨
 *   <li>페이지네이션 fragment 존재 (10건 초과 시)
 *   <li>empty state 라벨
 *   <li>Thymeleaf 표현식 잔존 0건
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminUserListRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor sysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  @Test
  void GET_admin_users_renders_shell_and_action_bar() throws Exception {
    mockMvc
        .perform(get("/admin/users").with(sysadmin()))
        .andExpect(status().isOk())
        // 페이지 타이틀
        .andExpect(content().string(containsString("사용자 관리")))
        // GNB 활성
        .andExpect(content().string(containsString("class=\"admin-nav-link active\">사용자 관리</a>")))
        // 검색 인풋
        .andExpect(content().string(containsString("class=\"admin-program-search-input\"")))
        .andExpect(content().string(containsString("placeholder=\"이름, 이메일 검색\"")))
        // Thymeleaf 표현식 잔존 없음
        .andExpect(content().string(not(containsString("${"))))
        .andExpect(content().string(not(containsString("th:each"))));
  }

  @Test
  void GET_admin_users_renders_role_filter_4_options() throws Exception {
    // Qn-A 이월 결정 반영: 4옵션 (전체 · 시스템 관리자 · 관리자 · 사용자)
    mockMvc
        .perform(get("/admin/users").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">전체</a>")))
        .andExpect(content().string(containsString(">시스템 관리자</a>")))
        .andExpect(content().string(containsString(">관리자</a>")))
        .andExpect(content().string(containsString(">사용자</a>")));
  }

  @Test
  void GET_admin_users_role_filter_url_reflected_and_tab_active() throws Exception {
    mockMvc
        .perform(get("/admin/users").param("role", "SYSTEM_ADMIN").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(
                    containsString(
                        "class=\"admin-program-tab admin-program-tab--active\">시스템 관리자</a>")));
  }

  @Test
  void GET_admin_users_column_headers_9_columns() throws Exception {
    // spec §4 컬럼 스키마: No · 이름 · 이메일 · 성별 · 권한 · 핸드폰 · 가입일 · 최근접속 · 상태
    // Qn-9 이월: checkbox 컬럼은 제거된 상태 (bulk UI = A8 착수 시 재도입)
    mockMvc
        .perform(get("/admin/users").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">No.</span>")))
        .andExpect(content().string(containsString(">이름</span>")))
        .andExpect(content().string(containsString(">이메일</span>")))
        .andExpect(content().string(containsString(">성별</span>")))
        .andExpect(content().string(containsString(">권한</span>")))
        .andExpect(content().string(containsString(">핸드폰</span>")))
        .andExpect(content().string(containsString(">가입일</span>")))
        .andExpect(content().string(containsString(">최근접속</span>")))
        .andExpect(content().string(containsString(">상태</span>")));
  }

  @Test
  void GET_admin_users_search_empty_result_shows_empty_state() throws Exception {
    mockMvc
        .perform(get("/admin/users").param("q", "___NO_MATCH_XYZ___").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("검색 결과가 없어요")))
        // 검색어 유지
        .andExpect(content().string(containsString("___NO_MATCH_XYZ___")));
  }

  @Test
  void GET_admin_users_pagination_rendered_when_more_than_10() throws Exception {
    // 시드에 30 seed + 3 admin ≥ 10 → 페이지네이션 노출
    mockMvc
        .perform(get("/admin/users").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-notice-paging")))
        .andExpect(content().string(containsString("admin-page-btn")));
  }

  @Test
  void GET_admin_users_role_badge_styles_rendered() throws Exception {
    // roleCfg 매핑 반영: admin-role-badge--system_admin / --center_admin / --user
    // 시드 30+ 명 / 페이지 10건이라 기본 목록엔 최신 seed 만 노출됨 → role filter 로 SYSTEM_ADMIN 만 조회.
    mockMvc
        .perform(get("/admin/users").param("role", "SYSTEM_ADMIN").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-role-badge--system_admin")));
  }

  @Test
  void GET_admin_users_status_badge_active_for_seeded_users() throws Exception {
    // V16 default TRUE → 모든 시드 유저는 활성 상태 렌더
    mockMvc
        .perform(get("/admin/users").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-user-status-badge--active")))
        .andExpect(content().string(containsString(">활성</span>")));
  }
}
