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
 * A4 admin-program-detail (2026-09-15) — 신청 관리 목록 화면 렌더 검증.
 *
 * <p>F0h-c2 사고 (Thymeleaf 파싱·SpEL 표현식 잔존) 재발 방지를 위해 필수. 요약 배지 5종, 필터 세그먼트, 테이블 헤더, data-testid 셀렉터
 * 존재를 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminApplicationListRenderTest {

  private static final long PROGRAM_ID = 1L;

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor sysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  @Test
  void GET_applications_목록_렌더_기본() throws Exception {
    mockMvc
        .perform(get("/admin/programs/" + PROGRAM_ID + "/applications").with(sysadmin()))
        .andExpect(status().isOk())
        // 페이지 타이틀
        .andExpect(content().string(containsString("신청 관리")))
        // 요약 배지 5종
        .andExpect(content().string(containsString("data-testid=\"summary-total\"")))
        .andExpect(content().string(containsString("data-testid=\"summary-pending\"")))
        .andExpect(content().string(containsString("data-testid=\"summary-approved\"")))
        .andExpect(content().string(containsString("data-testid=\"summary-rejected\"")))
        .andExpect(content().string(containsString("data-testid=\"summary-cancelled\"")))
        // 필터 세그먼트
        .andExpect(content().string(containsString("data-testid=\"filter-all\"")))
        .andExpect(content().string(containsString("data-testid=\"filter-pending\"")))
        .andExpect(content().string(containsString("data-testid=\"filter-approved\"")))
        .andExpect(content().string(containsString("data-testid=\"filter-rejected\"")))
        .andExpect(content().string(containsString("data-testid=\"filter-cancelled\"")))
        // 검색 인풋
        .andExpect(content().string(containsString("data-testid=\"filter-q\"")))
        // 브레드크럼
        .andExpect(content().string(containsString("← 프로그램 상세로")))
        // GNB 활성 (헤더 fragment 로직)
        .andExpect(content().string(containsString("admin-nav-link active")))
        // Thymeleaf 표현식 잔존 없음
        .andExpect(content().string(not(containsString("${"))))
        .andExpect(content().string(not(containsString("th:each"))));
  }

  @Test
  void GET_applications_status_filter_활성_클래스_적용() throws Exception {
    mockMvc
        .perform(
            get("/admin/programs/" + PROGRAM_ID + "/applications?status=PENDING").with(sysadmin()))
        .andExpect(status().isOk())
        // PENDING 필터 링크에 --active class 부착 (기본은 admin-program-tab, 활성은 --active)
        .andExpect(content().string(containsString("data-testid=\"filter-pending\">대기</a>")))
        .andExpect(content().string(containsString("admin-program-tab--active")));
  }

  @Test
  void GET_applications_없는_프로그램_404() throws Exception {
    mockMvc
        .perform(get("/admin/programs/999999/applications").with(sysadmin()))
        .andExpect(status().isNotFound());
  }

  @Test
  void GET_applications_익명_login_리다이렉트() throws Exception {
    mockMvc
        .perform(get("/admin/programs/" + PROGRAM_ID + "/applications"))
        .andExpect(status().is3xxRedirection());
  }
}
