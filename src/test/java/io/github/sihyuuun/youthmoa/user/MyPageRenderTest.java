package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * MyPage 렌더 검증.
 *
 * <ul>
 *   <li>/mypage · /mypage?tab=favorites Thymeleaf 파싱 오류 없이 200
 *   <li>취소 모달 자동 노출 여부 — {@code <div class="mypage-modal" hidden>} 이 렌더돼야 하고 초기 상태 hidden
 *   <li>프로필 요약 태그 prototype 매칭 — "관심 지역 · X" + "관심 · Y·Z" 형식 (F-signup-03)
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class MyPageRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor authed() {
    User u = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor authedAs(String email) {
    User u = userRepository.findByEmail(email).orElseThrow();
    return user(new UserPrincipal(u));
  }

  @Test
  void mypage_history_렌더() throws Exception {
    mockMvc.perform(get("/mypage").with(authed())).andExpect(status().isOk());
  }

  @Test
  void mypage_favorites_렌더() throws Exception {
    mockMvc.perform(get("/mypage?tab=favorites").with(authed())).andExpect(status().isOk());
  }

  /** 취소 모달은 hidden 상태로 렌더돼야 함 (CSS `:not([hidden])` 로 방어). */
  @Test
  void mypage_history_취소모달_초기_hidden() throws Exception {
    mockMvc
        .perform(get("/mypage").with(authed()))
        .andExpect(status().isOk())
        // 모달 div 자체는 존재해야 함
        .andExpect(content().string(containsString("id=\"cancelModal\"")))
        // hidden 속성 부여됨
        .andExpect(content().string(containsString("class=\"mypage-modal\" hidden")));
  }

  /** F-signup-03: 프로필 요약 태그 = "관심 지역 · X" + "관심 · Y·Z" (prototype.tsx L1237 매칭). */
  @Test
  @Transactional
  void mypage_프로필_요약_태그_prototype_매칭() throws Exception {
    // seed2 유저에 관심 지역·분야 직접 세팅 후 /mypage 조회
    User u = userRepository.findByEmail("seed2@youth-moa.test").orElseThrow();
    u.updateInterests(Set.of("수원시"), Set.of("취업·역량", "창업"));
    userRepository.saveAndFlush(u);

    String body =
        mockMvc
            .perform(get("/mypage").with(authedAs("seed2@youth-moa.test")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // prototype 태그 형식 확인
    assertThat(body).contains("관심 지역 · 수원시");
    // 분야는 · join. 순서는 Set 이라 확정 불가 → 두 카테고리 모두 포함 검증
    assertThat(body).contains("관심 · ");
    assertThat(body).contains("취업·역량");
    assertThat(body).contains("창업");
    // 옛 해시 접두어 잔재 없음
    assertThat(body).doesNotContain("#수원시");
  }

  /** 관심 정보 미설정 시 태그 미노출. */
  @Test
  @Transactional
  void mypage_관심정보_없으면_태그_미노출() throws Exception {
    // seed3 는 시드 시 interests 안 세팅 (DataInitializer 참고)
    mockMvc
        .perform(get("/mypage").with(authedAs("seed3@youth-moa.test")))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("관심 지역 ·"))))
        .andExpect(content().string(not(containsString("관심 ·"))));
  }
}
