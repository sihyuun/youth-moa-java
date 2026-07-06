package io.github.sihyuuun.youthmoa.render;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.sihyuuun.youthmoa.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 실 Thymeleaf 렌더까지 실행하는 통합 렌더 테스트.
 *
 * <p>{@code @WebMvcTest} 는 view name / model attribute 만 검증하고 실제 파싱·EL 평가를 수행하지 않는다. 2026-07-03
 * 세션에서 아래 사고 3종이 WebMvcTest 를 통과했으나 실 브라우저·E2E 에서만 발견됨:
 *
 * <ul>
 *   <li>{@code <sec:authentication property="principal.displayName"/>} 태그가 리터럴로 렌더됨
 *       (thymeleaf-extras-springsecurity6 + Spring Security 7 조합)
 *   <li>{@code @AssertTrue} 오류 필드가 {@code privacyAccepted} 로 파생되어 템플릿의 {@code
 *       #fields.hasErrors('privacyAgreed')} 가 항상 false → 에러 메시지 미렌더
 *   <li>{@code application} 모델 attribute 이름이 ServletContext scope 예약어와 shadowing 되어 {@code #Anull}
 *       렌더
 * </ul>
 *
 * <p>본 테스트는 e2e 프로파일 (H2 in-memory + DataInitializer 시드) 로 부팅해 실 응답 HTML 을 assert 한다. 새 화면 추가 시 회귀
 * 방지용 시그니처를 계속 축적한다. Playwright E2E 는 사용자 여정을, 본 테스트는 서버측 렌더 시그니처를 커버 (역할 분리).
 *
 * <p><b>매처 원칙</b>: 넓은 "정합성" 매처 (예: 모든 {@code class="null "} 탐색) 는 false-positive 가 잦음. 대신 실 사고
 * 시그니처를 그대로 assert.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class PageRenderIntegrationTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserService userService;

  @Test
  void 홈_비로그인_렌더() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        // Hero 재설계 (F0e-hero-refresh) 문안 렌더 확인
        .andExpect(content().string(containsString("청년의 모든 기회를")))
        // sec:authentication element 태그가 리터럴 렌더되지 않았는지 (2026-07-03 사고 시그니처)
        .andExpect(content().string(not(containsString("<sec:authentication"))))
        // 비인증: 로그인 아이콘 노출
        .andExpect(
            content().string(containsString("class=\"header-icon-btn header-icon-btn--primary\"")));
  }

  @Test
  void 홈_로그인_상태에서_displayName_이_렌더된다() throws Exception {
    UserDetails principal = userService.loadUserByUsername("seed30@youth-moa.test");
    mockMvc
        .perform(get("/").with(user(principal)))
        .andExpect(status().isOk())
        // sec:authentication 태그 리터럴 렌더되면 안 됨 — 2026-07-03 사고 시그니처
        .andExpect(content().string(not(containsString("<sec:authentication"))))
        // 헤더 사용자 이름 실제 렌더 확인
        .andExpect(content().string(containsString("시드유저30")));
  }

  @Test
  void 공지_목록_카테고리_탭_5개_렌더() throws Exception {
    mockMvc
        .perform(get("/notices"))
        .andExpect(status().isOk())
        // F0g pill 탭 렌더 (전체 + 4)
        .andExpect(content().string(containsString("class=\"notice-tab active\"")))
        // HTMX outerHTML swap 대상 wrapper 렌더
        .andExpect(content().string(containsString("notice-list-region")))
        // 미평가 Thymeleaf 표현식 잔존 방지
        .andExpect(content().string(not(containsString("th:each"))));
  }

  @Test
  void 공지_상세_HTML_본문과_이전다음_렌더() throws Exception {
    mockMvc
        .perform(get("/notices/1"))
        .andExpect(status().isOk())
        // th:utext 로 HTML 본문 렌더
        .andExpect(content().string(containsString("notice-detail-body")))
        // 이전/다음 네비 (empty or 링크)
        .andExpect(content().string(containsString("notice-adjacent")))
        // sec:authentication 시그니처 재확인 (헤더 fragment 렌더 경로)
        .andExpect(content().string(not(containsString("<sec:authentication"))));
  }

  @Test
  void 신청_완료_페이지_myApplication_예약어_회피_및_필드_렌더() throws Exception {
    // seed1 은 program1 에 APPROVED 시드 (DataInitializer). Application id=1 은 시드 순서 상 seed1 의 첫 번째.
    UserDetails seed1 = userService.loadUserByUsername("seed1@youth-moa.test");
    mockMvc
        .perform(get("/apply/complete").param("applicationId", "1").with(user(seed1)))
        .andExpect(status().isOk())
        // 2026-07-02 사고: application 모델 attribute 이름 shadowing → "#Anull", "신청일시 null"
        .andExpect(content().string(not(containsString("#Anull"))))
        .andExpect(content().string(not(containsString("신청일시 null"))))
        // 정상 렌더 시그니처: #A{숫자}, 신청일시 yyyy-MM-dd
        .andExpect(content().string(matchesRegex("(?s).*#A\\d+.*")))
        .andExpect(content().string(matchesRegex("(?s).*신청일시 \\d{4}-\\d{2}-\\d{2}.*")));
  }

  @Test
  void 신청_폼_privacy_에러_메시지_렌더() throws Exception {
    // seed30 (미신청) 이 program 3 apply POST 로 privacyAgreed=false 제출 → 검증 실패 재렌더.
    // 2026-07-03 사고: @AssertTrue 가 getter 에 붙어 privacyAccepted 로 파생되어 hasErrors('privacyAgreed') =
    // false → 메시지 미렌더
    UserDetails seed30 = userService.loadUserByUsername("seed30@youth-moa.test");
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/programs/3/apply")
                .param("applyReason", "지원 동기 10자 이상 입력 문장입니다.")
                // privacyAgreed 미체크
                .with(user(seed30))
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().isOk())
        // 에러 메시지가 화면에 노출되어야 함 (템플릿 <p th:errors="*{privacyAgreed}"> 렌더)
        .andExpect(content().string(containsString("개인정보 수집 동의가 필요합니다.")));
  }
}
