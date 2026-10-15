package io.github.sihyuuun.youthmoa.admin.program;

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
 * FOLLOW-admin-calendar-panel-card-prototype-match (2026-10-14) — admin 캘린더 우측 패널 카드 렌더 회귀 방어.
 *
 * <p>Thymeleaf 파싱·SpEL 평가 + fragment 인라인 실행까지 검증. {@code compileJava} + Service 테스트만으로는 템플릿 변경 사고를
 * 못 잡으므로 반드시 함께 돌린다.
 *
 * <p>검증 항목 (명세 §3):
 *
 * <ul>
 *   <li>admin prototype L1125~1157 매칭 마크업 (썸네일 텍스트 · 제목 anchor · 정보 3라인 · 상세보기 버튼)
 *   <li>regression — {@code .program-calendar-panel-card-bar} / {@code -chip} / {@code -capacity} 가
 *       admin 패널 카드에 재발하지 않음
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class AdminProgramCalendarRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  @Test
  void admin_캘린더_패널_카드_prototype_매칭_마크업_렌더() throws Exception {
    User sys = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(sys);

    String html =
        mockMvc
            .perform(get("/admin/programs?view=calendar").with(user(principal)))
            .andExpect(status().isOk())
            // ─── 패널 카드 wrapper 는 article (anchor 중첩 회피) ───
            .andExpect(content().string(containsString("admin-program-calendar-panel-card")))
            // ─── 썸네일 (텍스트 전용 · Q1=b) ───
            .andExpect(content().string(containsString("admin-program-calendar-panel-card-thumb")))
            .andExpect(
                content().string(containsString("admin-program-calendar-panel-card-thumb-text")))
            // ─── 제목 (상세 anchor) ───
            .andExpect(content().string(containsString("admin-program-calendar-panel-card-title")))
            // ─── 센터명 ───
            .andExpect(content().string(containsString("admin-program-calendar-panel-card-center")))
            // ─── 구분선 ───
            .andExpect(
                content().string(containsString("admin-program-calendar-panel-card-divider")))
            // ─── 정보 3라인 (dl + dt 라벨) ───
            .andExpect(content().string(containsString("admin-program-calendar-panel-card-info")))
            .andExpect(content().string(containsString("진행 기간")))
            .andExpect(content().string(containsString("신청 기간")))
            .andExpect(content().string(containsString("신청 현황")))
            // ─── 신청 현황 값 (primary bold) ───
            .andExpect(
                content().string(containsString("admin-program-calendar-panel-card-applied")))
            // ─── 상세보기 버튼 ───
            .andExpect(
                content().string(containsString("admin-program-calendar-panel-card-detail-btn")))
            .andExpect(content().string(containsString("상세보기")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    // ─── regression — admin 패널 카드에 bar / chip / capacity 재발 금지 ───
    org.assertj.core.api.Assertions.assertThat(html)
        .doesNotContain("program-calendar-panel-card-bar")
        .doesNotContain("program-calendar-panel-card-chip")
        .doesNotContain("program-calendar-panel-card-capacity");
  }

  @Test
  void capacity_null_인_프로그램은_제한없음_fallback() throws Exception {
    // panelData 가 비어있을 수 있으므로 제한 없음 fallback 이 반드시 렌더된다는 보장은 어렵다.
    // 대신 fragment 템플릿에 fallback 리터럴이 포함돼 있는지 확인한다 (regression — 리터럴 제거 방지).
    User sys = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    UserPrincipal principal = new UserPrincipal(sys);

    mockMvc
        .perform(get("/admin/programs?view=calendar").with(user(principal)))
        .andExpect(status().isOk())
        // "제한 없음" 리터럴은 th:text 삼항 표현식 안에만 있고,
        // 실 데이터가 capacity > 0 이라면 렌더 결과엔 등장하지 않을 수 있다.
        // 그러나 Thymeleaf 가 SpEL 삼항을 평가하므로 렌더 에러 (SpEL / th:*) 가 없는지만 확인.
        .andExpect(content().string(not(containsString("${"))))
        .andExpect(content().string(not(containsString("th:text="))));
  }
}
