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
import io.github.sihyuuun.youthmoa.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * A5 admin-users (2026-09-15): {@code /admin/users/{id}} 상세 Thymeleaf 실 렌더 회귀 방어.
 *
 * <p>prototype L1774~1943 대비: 2컬럼 그리드 + 좌 프로필 카드 + 우 신청 이력 4탭 + 권한 radio + admin-note + danger
 * zone. Qn-B 페이지 결정.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Transactional
class AdminUserDetailRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  /**
   * 각 테스트 전에 신규 활성 유저를 생성해 크로스-테스트 오염 회피. 시드 유저(seed1 등)를 재사용하면 다른 테스트 클래스에서 `deactivate` 를 통해 남긴
   * 상태(deactivated_by=proxy)가 lazyInit 예외를 유발할 수 있음. 클래스에 @Transactional 이 걸려 있어 이 유저는 테스트 후 자동
   * 롤백된다.
   */
  private User target;

  @BeforeEach
  void createFreshTarget() {
    target =
        userRepository.save(
            User.builder()
                .email("detail-render-fixture@youth-moa.test")
                .password("$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
                .name("상세렌더픽스처")
                .role(UserRole.USER)
                .build());
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor sysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  private Long seedUserId() {
    return target.getId();
  }

  @Test
  void GET_admin_user_detail_renders_2col_grid_and_profile() throws Exception {
    Long id = seedUserId();
    mockMvc
        .perform(get("/admin/users/" + id).with(sysadmin()))
        .andExpect(status().isOk())
        // 2컬럼 그리드
        .andExpect(content().string(containsString("admin-user-detail-grid")))
        // 좌 프로필 카드
        .andExpect(content().string(containsString("회원 정보")))
        .andExpect(content().string(containsString("이메일 (아이디)")))
        .andExpect(content().string(containsString("이름")))
        .andExpect(content().string(containsString("성별")))
        .andExpect(content().string(containsString("생년월일")))
        .andExpect(content().string(containsString("핸드폰 번호")))
        .andExpect(content().string(containsString("주소")))
        // 우 신청 이력 카드
        .andExpect(content().string(containsString("프로그램 신청 현황")))
        // Thymeleaf 표현식 잔존 없음
        .andExpect(content().string(not(containsString("${"))));
  }

  @Test
  void GET_admin_user_detail_renders_4_application_tabs() throws Exception {
    // prototype L1889~1894: 전체 · 승인 · 반려 · 취소
    Long id = seedUserId();
    mockMvc
        .perform(get("/admin/users/" + id).with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(">전체</a>")))
        .andExpect(content().string(containsString(">승인</a>")))
        .andExpect(content().string(containsString(">반려</a>")))
        .andExpect(content().string(containsString(">취소</a>")));
  }

  @Test
  void GET_admin_user_detail_tab_APPROVED_marks_active() throws Exception {
    Long id = seedUserId();
    mockMvc
        .perform(get("/admin/users/" + id).param("tab", "APPROVED").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .string(containsString("class=\"admin-user-tab admin-user-tab--active\">승인</a>")));
  }

  @Test
  void GET_admin_user_detail_renders_role_radios_3_options() throws Exception {
    // spec §3 결정: 시스템 관리자 · 관리자 · 사용자 radio 3종
    Long id = seedUserId();
    mockMvc
        .perform(get("/admin/users/" + id).with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("name=\"role\"")))
        .andExpect(content().string(containsString("value=\"SYSTEM_ADMIN\"")))
        .andExpect(content().string(containsString("value=\"CENTER_ADMIN\"")))
        .andExpect(content().string(containsString("value=\"USER\"")));
  }

  @Test
  void GET_admin_user_detail_renders_admin_note_form() throws Exception {
    Long id = seedUserId();
    mockMvc
        .perform(get("/admin/users/" + id).with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("관리자 메모")))
        .andExpect(content().string(containsString("name=\"adminNote\"")))
        .andExpect(content().string(containsString("maxlength=\"1000\"")));
  }

  @Test
  void GET_admin_user_detail_renders_danger_zone_and_deactivate_modal() throws Exception {
    Long id = seedUserId();
    mockMvc
        .perform(get("/admin/users/" + id).with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-user-danger-zone")))
        .andExpect(content().string(containsString("위험 존")))
        .andExpect(content().string(containsString("사용자 차단")))
        // 차단 모달 markup
        .andExpect(content().string(containsString("id=\"deactivateModal\"")))
        .andExpect(content().string(containsString("차단 사유")))
        .andExpect(content().string(containsString("name=\"reason\"")))
        // 사유 필수 표시
        .andExpect(content().string(containsString("required")));
  }

  @Test
  void GET_admin_user_detail_self_disables_role_and_deactivate_buttons() throws Exception {
    // sysadmin 이 자기 자신 상세 진입 시 role 변경·차단 버튼 disabled + "본인 계정" 힌트 노출
    Long selfId = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow().getId();

    mockMvc
        .perform(get("/admin/users/" + selfId).with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("본인 계정")))
        .andExpect(content().string(containsString("disabled")));
  }

  @Test
  void GET_admin_user_detail_missing_returns_404() throws Exception {
    mockMvc.perform(get("/admin/users/999999").with(sysadmin())).andExpect(status().isNotFound());
  }
}
