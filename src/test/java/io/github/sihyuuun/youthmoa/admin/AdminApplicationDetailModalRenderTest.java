package io.github.sihyuuun.youthmoa.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
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
 * A4 admin-program-detail (2026-09-15) — 신청 상세 모달 fragment 렌더 검증. Qn-B A 모달 방식.
 *
 * <p>승인·반려·강제 취소 form 3개 + 담당자 의견 form + F0c 답변 렌더 + 신청자 정보 grid 를 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminApplicationDetailModalRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;
  @Autowired ApplicationRepository applicationRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor sysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  private Application seededApplication() {
    return applicationRepository.findAll().stream()
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("no seeded application"));
  }

  @Test
  void GET_application_detail_렌더_기본_및_form_3개() throws Exception {
    Application app = seededApplication();
    Long pid = app.getProgram().getId();
    mockMvc
        .perform(get("/admin/programs/" + pid + "/applications/" + app.getId()).with(sysadmin()))
        .andExpect(status().isOk())
        // 페이지 타이틀 (fragment 이지만 host 페이지 렌더링)
        .andExpect(content().string(containsString("프로그램 신청 상세")))
        // 신청자 정보 grid
        .andExpect(content().string(containsString("data-testid=\"detail-user-info\"")))
        .andExpect(content().string(containsString("이름")))
        .andExpect(content().string(containsString("이메일")))
        .andExpect(content().string(containsString("접수일시")))
        .andExpect(content().string(containsString("참여횟수")))
        // 상태 배지
        .andExpect(content().string(containsString("data-testid=\"detail-status-badge\"")))
        // 상태 변경 form 3개 (승인/반려/강제 취소)
        .andExpect(content().string(containsString("data-testid=\"btn-approve\"")))
        .andExpect(content().string(containsString("data-testid=\"btn-reject\"")))
        .andExpect(content().string(containsString("data-testid=\"btn-cancel\"")))
        .andExpect(content().string(containsString("data-testid=\"reject-reason-input\"")))
        .andExpect(content().string(containsString("data-testid=\"cancel-reason-input\"")))
        // 담당자 의견 form
        .andExpect(content().string(containsString("data-testid=\"admin-note-input\"")))
        .andExpect(content().string(containsString("data-testid=\"btn-save-note\"")))
        .andExpect(content().string(containsString("사용자에게 노출됩니다")))
        // 뒤로가기 링크
        .andExpect(content().string(containsString("data-testid=\"back-to-list\"")))
        // CSRF 토큰 hidden input (form POST 용)
        .andExpect(content().string(containsString("_csrf")))
        // Thymeleaf 표현식 잔존 없음
        .andExpect(content().string(not(containsString("${"))))
        .andExpect(content().string(not(containsString("th:each"))));
  }

  @Test
  void GET_application_detail_없는_신청_404() throws Exception {
    Long pid = seededApplication().getProgram().getId();
    mockMvc
        .perform(get("/admin/programs/" + pid + "/applications/999999").with(sysadmin()))
        .andExpect(status().isNotFound());
  }

  @Test
  void GET_application_detail_익명_리다이렉트() throws Exception {
    Application app = seededApplication();
    Long pid = app.getProgram().getId();
    mockMvc
        .perform(get("/admin/programs/" + pid + "/applications/" + app.getId()))
        .andExpect(status().is3xxRedirection());
  }
}
