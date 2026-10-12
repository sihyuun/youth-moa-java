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
 * A2 (2026-09-09) — 관리자 프로그램 목록 Thymeleaf 실 렌더 회귀 방어.
 *
 * <p>F0h-c2 회고 반영: {@code compileJava} + {@code JpaMappingTest} 만으로는 Thymeleaf 파싱·SpEL·fragment 렌더
 * 이슈를 감지 못 함.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminProgramListRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor sysadmin() {
    User u = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor centerAdmin() {
    User u = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  // ================= 목록 렌더 =================

  @Test
  void GET_admin_programs_렌더_액션바_필터_5종() throws Exception {
    mockMvc
        .perform(get("/admin/programs").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("프로그램 관리")))
        // 필터 탭 5종 라벨
        .andExpect(content().string(containsString(">전체</a>")))
        .andExpect(content().string(containsString(">모집중</a>")))
        .andExpect(content().string(containsString(">진행예정</a>")))
        .andExpect(content().string(containsString(">종료</a>")))
        .andExpect(content().string(containsString(">운영중단</a>")))
        // 검색 인풋
        .andExpect(content().string(containsString("class=\"admin-program-search-input\"")))
        .andExpect(content().string(containsString("placeholder=\"프로그램명, 센터 검색\"")))
        // 컬럼 헤더 9종
        .andExpect(content().string(containsString(">프로그램명</span>")))
        .andExpect(content().string(containsString(">카테고리</span>")))
        .andExpect(content().string(containsString(">청년센터</span>")))
        .andExpect(content().string(containsString(">상태</span>")))
        .andExpect(content().string(containsString(">신청기간</span>")))
        .andExpect(content().string(containsString(">신청현황</span>")))
        .andExpect(content().string(containsString(">조회수</span>")))
        .andExpect(content().string(containsString(">등록일</span>")))
        .andExpect(content().string(containsString(">관리</span>")))
        // 편집 링크
        .andExpect(content().string(containsString(">편집</a>")))
        // GNB 활성
        .andExpect(content().string(containsString("class=\"admin-nav-link active\">프로그램 관리</a>")))
        // Thymeleaf 표현식 잔존 없음
        .andExpect(content().string(not(containsString("${"))))
        .andExpect(content().string(not(containsString("th:each"))));
  }

  @Test
  void GET_admin_programs_필터_적용_URL() throws Exception {
    mockMvc
        .perform(get("/admin/programs").param("status", "OPEN").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-program-tab--active")))
        // OPEN 탭이 active 로 표시돼야 함
        .andExpect(
            content()
                .string(
                    containsString(
                        "class=\"admin-program-tab admin-program-tab--active\">모집중</a>")));
  }

  @Test
  void GET_admin_programs_검색_반영() throws Exception {
    mockMvc
        .perform(get("/admin/programs").param("q", "존재하지않을검색어XYZ123").with(sysadmin()))
        .andExpect(status().isOk())
        // 검색 결과가 없어요 문구
        .andExpect(content().string(containsString("검색 결과가 없어요")))
        // 검색어가 인풋에 유지
        .andExpect(content().string(containsString("존재하지않을검색어XYZ123")));
  }

  // ================= RBAC =================

  @Test
  void GET_admin_programs_익명_login_리다이렉트() throws Exception {
    mockMvc.perform(get("/admin/programs")).andExpect(status().is3xxRedirection());
  }

  @Test
  void GET_admin_programs_centerAdmin_200_자기센터만() throws Exception {
    mockMvc
        .perform(get("/admin/programs").with(centerAdmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("프로그램 관리")));
  }

  // ================= D-admin-calendar (2026-10-12) =================

  @Test
  void GET_admin_programs_view_토글_세그먼트_3버튼_렌더() throws Exception {
    mockMvc
        .perform(get("/admin/programs").with(sysadmin()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("admin-program-view-toggle")))
        .andExpect(content().string(containsString("data-view=\"list\"")))
        .andExpect(content().string(containsString("data-view=\"card\"")))
        .andExpect(content().string(containsString("data-view=\"calendar\"")))
        // 기본 view=list → list 버튼이 active (class·속성 순서 Thymeleaf 가 결정하므로 fragment 단위로만 검증)
        .andExpect(content().string(containsString("admin-program-view-btn--active")));
  }

  @Test
  void GET_admin_programs_view_calendar_42셀_grid_렌더() throws Exception {
    mockMvc
        .perform(get("/admin/programs").param("view", "calendar").with(sysadmin()))
        .andExpect(status().isOk())
        // 캘린더 fragment 가 포함돼야 함
        .andExpect(content().string(containsString("admin-program-calendar-layout")))
        .andExpect(content().string(containsString("program-calendar-grid")))
        .andExpect(content().string(containsString("admin-program-calendar-panel")))
        // list 테이블은 숨겨짐
        .andExpect(content().string(not(containsString("admin-program-list-card"))))
        // calendar 버튼이 active
        .andExpect(content().string(containsString("data-view=\"calendar\"")))
        // Thymeleaf 표현식 잔존 없음
        .andExpect(content().string(not(containsString("${"))));
  }

  @Test
  void GET_admin_programs_view_card_placeholder_flash() throws Exception {
    mockMvc
        .perform(get("/admin/programs").param("view", "card").with(sysadmin()))
        .andExpect(status().isOk())
        // placeholder flash
        .andExpect(content().string(containsString("카드 뷰는 별도 티켓에서 제공돼요")))
        // list 렌더는 유지
        .andExpect(content().string(containsString("admin-program-list-card")));
  }

  @Test
  void GET_admin_programs_view_calendar_q_status_pass_through_유지() throws Exception {
    mockMvc
        .perform(
            get("/admin/programs")
                .param("view", "calendar")
                .param("q", "교육")
                .param("status", "SUSPENDED")
                .with(sysadmin()))
        .andExpect(status().isOk())
        // 캘린더 nav 링크에 q·status 유지
        .andExpect(content().string(containsString("q=%EA%B5%90%EC%9C%A1")))
        .andExpect(content().string(containsString("status=SUSPENDED")));
  }
}
