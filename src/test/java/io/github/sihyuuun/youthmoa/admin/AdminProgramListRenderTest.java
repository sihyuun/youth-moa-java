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
}
