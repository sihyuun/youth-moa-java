package io.github.sihyuuun.youthmoa.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramRepository;
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
 * FOLLOW-admin-program-detail-readonly (2026-10-07): {@code GET /admin/programs/{id}} 가 다시
 * read-only 상세 페이지로 분리됨. 편집 폼은 {@link AdminProgramFormRenderTest} 에서 {@code /{id}/edit} 로 검증.
 *
 * <p>ym-impl 3차 (2026-10-07): verify UNVERIFIED-U1 해소 — CENTER_ADMIN own-center 200 + cross-center
 * 403 2 TC 추가. 기존 Service 레벨 {@link
 * AdminProgramServiceTest#find_centerAdmin_wrongOrganization_throwsIllegalAccess} 는 scopeSpec() 로직만
 * 커버 → render + Controller.detail() 의 IllegalAccessError→AccessDeniedException 승격·403 응답까지 자동 회귀 방어
 * 공백이었음.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminProgramDetailRenderTest {

  private static final long PROGRAM_ID = 1L;

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;
  @Autowired ProgramRepository programRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor sysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor centerAdmin() {
    User u = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  /** center1 (centers.get(0)) 소속 own-center program id 를 시드에서 동적으로 찾는다. */
  private Long findOwnCenterProgramId() {
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    Long centerId = center1.getCenter().getId();
    return programRepository.findAll().stream()
        .filter(p -> p.getCenter() != null && centerId.equals(p.getCenter().getId()))
        .map(Program::getId)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("center1 소속 프로그램 시드 누락 — DataInitializer 확인"));
  }

  /** center1 과 다른 센터 소속 program id 를 시드에서 동적으로 찾는다 (cross-center 403 검증용). */
  private Long findCrossCenterProgramId() {
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    Long centerId = center1.getCenter().getId();
    return programRepository.findAll().stream()
        .filter(p -> p.getCenter() != null && !centerId.equals(p.getCenter().getId()))
        .map(Program::getId)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("다른 센터 소속 프로그램 시드 누락 — DataInitializer 확인"));
  }

  @Test
  void GET_admin_program_detail_렌더_상세_헤더_및_주요_마크업() throws Exception {
    mockMvc
        .perform(get("/admin/programs/" + PROGRAM_ID).with(sysadmin()))
        .andExpect(status().isOk())
        // 상세 전용 wrapper
        .andExpect(content().string(containsString("admin-program-detail-page")))
        .andExpect(content().string(containsString("admin-program-detail-header")))
        // Q1: 수정 CTA → /edit 서브경로
        .andExpect(content().string(containsString("/admin/programs/" + PROGRAM_ID + "/edit")))
        // Q6: 신청 현황 보기 링크
        .andExpect(content().string(containsString("data-testid=\"link-applications\"")))
        .andExpect(
            content().string(containsString("/admin/programs/" + PROGRAM_ID + "/applications")))
        // Q5: ⋯ 더보기 삭제 메뉴 (복제는 미노출)
        .andExpect(content().string(containsString("data-testid=\"detail-menu-delete\"")))
        .andExpect(content().string(not(containsString("data-testid=\"detail-menu-clone\""))))
        // Q8: watch-button 상세 헤더 전용 (detail-watch-btn)
        .andExpect(content().string(containsString("detail-watch-btn")))
        // 삭제 모달은 상세로 이관됨
        .andExpect(content().string(containsString("program-delete-modal")))
        // 상세는 form 폼이 아님 (편집은 /edit)
        .andExpect(content().string(not(containsString("class=\"admin-program-form\""))))
        // GNB 활성
        .andExpect(content().string(containsString("class=\"admin-nav-link active\">프로그램 관리</a>")))
        // Thymeleaf 표현식 잔존 없음
        .andExpect(content().string(not(containsString("${"))))
        .andExpect(content().string(not(containsString("th:each"))));
  }

  @Test
  void GET_admin_program_detail_없는_프로그램_404() throws Exception {
    mockMvc
        .perform(get("/admin/programs/999999").with(sysadmin()))
        .andExpect(status().isNotFound());
  }

  @Test
  void GET_admin_program_detail_익명_login_리다이렉트() throws Exception {
    mockMvc.perform(get("/admin/programs/" + PROGRAM_ID)).andExpect(status().is3xxRedirection());
  }

  // ================= UNVERIFIED-U1 (2026-10-07 ym-impl 3차): CENTER_ADMIN scope =================

  @Test
  void GET_admin_program_detail_centerAdmin_자기센터프로그램_200_렌더() throws Exception {
    Long ownId = findOwnCenterProgramId();
    mockMvc
        .perform(get("/admin/programs/" + ownId).with(centerAdmin()))
        .andExpect(status().isOk())
        // 상세 전용 wrapper 가 CENTER_ADMIN 에게도 동일하게 렌더
        .andExpect(content().string(containsString("admin-program-detail-page")))
        .andExpect(content().string(containsString("admin-program-detail-header")))
        // Thymeleaf 표현식 잔존 없음 (fragment 실행 완료)
        .andExpect(content().string(not(containsString("${"))));
  }

  @Test
  void GET_admin_program_detail_centerAdmin_타센터프로그램_403() throws Exception {
    Long otherId = findCrossCenterProgramId();
    mockMvc
        .perform(get("/admin/programs/" + otherId).with(centerAdmin()))
        // Controller.detail() 에서 IllegalAccessError → AccessDeniedException 승격 → 403
        .andExpect(status().isForbidden());
  }
}
