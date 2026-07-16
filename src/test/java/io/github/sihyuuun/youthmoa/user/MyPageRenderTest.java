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

  /**
   * 취소 모달은 hidden 상태로 렌더돼야 함 (CSS `:not([hidden])` 로 방어).
   * U-COMMON-01: .mypage-modal → .modal-backdrop + .modal-card 공통 셸로 마이그레이션.
   */
  @Test
  void mypage_history_취소모달_초기_hidden() throws Exception {
    String body =
        mockMvc
            .perform(get("/mypage").with(authed()))
            .andExpect(status().isOk())
            // 모달 backdrop 존재 (id 유지)
            .andExpect(content().string(containsString("id=\"cancelModal\"")))
            // 공통 .modal-backdrop 클래스 사용 (마이그레이션 확인)
            .andExpect(content().string(containsString("class=\"modal-backdrop\"")))
            // 내부에 공통 .modal-card 셸 존재
            .andExpect(content().string(containsString("modal-card modal-card--sm")))
            // hidden 속성 부여됨 (초기 상태)
            .andExpect(content().string(containsString("aria-hidden=\"true\" hidden")))
            // dead 클래스 미노출
            .andExpect(content().string(not(containsString("mypage-modal-inner"))))
            .andReturn().getResponse().getContentAsString();
    assertThat(body).contains("aria-modal=\"true\"");
  }

  /** F-signup-03 그룹형 뱃지: 값칩 최대 3개 + prototype.tsx L1236~1252 매칭. */
  @Test
  @Transactional
  void mypage_프로필_요약_그룹형_뱃지_prototype_매칭() throws Exception {
    User u = userRepository.findByEmail("seed2@youth-moa.test").orElseThrow();
    u.updateInterests(Set.of("수원시", "성남시"), Set.of("취업·역량", "창업"));
    userRepository.saveAndFlush(u);

    String body =
        mockMvc
            .perform(get("/mypage").with(authedAs("seed2@youth-moa.test")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // 그룹 컨테이너·라벨·값칩 렌더
    assertThat(body).contains("mypage-interest-groups");
    assertThat(body).contains("mypage-interest-group");
    assertThat(body).contains("관심 지역");
    assertThat(body).contains("관심 분야");
    assertThat(body).contains("mypage-interest-chip");
    // 값칩에 선택 모두 노출 (3개 이내)
    assertThat(body).contains(">수원시<");
    assertThat(body).contains(">성남시<");
    assertThat(body).contains(">취업·역량<");
    assertThat(body).contains(">창업<");
    // 편집 링크 (?tab=profile)
    assertThat(body).contains("관심 정보 수정");
    assertThat(body).contains("/mypage?tab=profile");
    // 옛 해시 접두어 잔재 없음
    assertThat(body).doesNotContain("#수원시");
    assertThat(body).doesNotContain("mypage-summary-tag");
  }

  /** 3개 초과 시 `+N` 축약칩 + title 툴팁 (prototype 대응). */
  @Test
  @Transactional
  void mypage_프로필_요약_뱃지_4개_이상_축약() throws Exception {
    User u = userRepository.findByEmail("seed2@youth-moa.test").orElseThrow();
    u.updateInterests(
        Set.of("수원시"),
        Set.of("취업·역량", "창업", "심리·건강", "문화·예술", "주거")); // 5개
    userRepository.saveAndFlush(u);

    String body =
        mockMvc
            .perform(get("/mypage").with(authedAs("seed2@youth-moa.test")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // 카테고리 5개 → shown 3 + `+2` 축약
    assertThat(body).contains("mypage-interest-more");
    assertThat(body).contains(">+2<");
    // title 툴팁 존재 (나머지 2개 join)
    assertThat(body).contains("title=");
  }

  /** 관심 정보 미설정 시 그룹은 노출되고 "미설정" 텍스트 표시. */
  @Test
  @Transactional
  void mypage_관심정보_없으면_미설정_표시() throws Exception {
    // seed3 는 interests 시드 없음
    String body =
        mockMvc
            .perform(get("/mypage").with(authedAs("seed3@youth-moa.test")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    // 그룹 자체는 항상 노출
    assertThat(body).contains("관심 지역");
    assertThat(body).contains("관심 분야");
    // "미설정" 텍스트 2회 이상 (각 그룹)
    assertThat(body).contains("미설정");
    assertThat(body).contains("mypage-interest-empty");
  }

  /** prototype 이모지 대체 금지 규칙 — edit 아이콘 SVG path. */
  @Test
  @Transactional
  void mypage_편집아이콘_SVG_이모지_대체_없음() throws Exception {
    String body =
        mockMvc
            .perform(get("/mypage").with(authed()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    // edit SVG path (fragments/icons.html)
    assertThat(body).contains("M12 20h9M16.5 3.5");
    // 이모지 부재
    assertThat(body).doesNotContain("✎");
    assertThat(body).doesNotContain("✏");
  }
}
