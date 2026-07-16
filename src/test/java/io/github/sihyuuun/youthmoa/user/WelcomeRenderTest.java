package io.github.sihyuuun.youthmoa.user;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * F-signup-03: WelcomeScreen 통합 렌더 검증.
 *
 * <p>e2e 프로파일 (H2 + 시드) 사용. spec §PR2 검증 항목: 12 지역 · 7 분야 · SVG path · noHeader · POST 302.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
class WelcomeRenderTest {

  @Autowired MockMvc mockMvc;
  @Autowired UserRepository userRepository;

  private org.springframework.test.web.servlet.request.RequestPostProcessor authed() {
    User u = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
    return user(new UserPrincipal(u));
  }

  @Test
  void GET_welcome_미로그인_login_리다이렉트() throws Exception {
    mockMvc.perform(get("/welcome")).andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/login"));
  }

  @Test
  void GET_welcome_로그인_200_지역30_분야7_SVG_렌더() throws Exception {
    String body =
        mockMvc
            .perform(get("/welcome").with(authed()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // 관심 지역 TOP 10 (isFeatured=true) — 상시 노출, prototype WELCOME_REGIONS_TOP
    String[] topRegions = {
      "수원시", "성남시", "고양시", "용인시", "부천시", "안양시", "안산시", "화성시", "남양주시", "평택시"
    };
    for (String r : topRegions) {
      org.junit.jupiter.api.Assertions.assertTrue(body.contains(r), "TOP 지역 누락: " + r);
    }
    // 관심 지역 MORE 20 (isFeatured=false) — 마크업 존재 (hidden 상태 렌더)
    String[] moreRegions = {
      "의정부시", "시흥시", "파주시", "김포시", "광명시", "광주시", "군포시", "오산시", "이천시", "양주시",
      "안성시", "구리시", "포천시", "의왕시", "하남시", "여주시", "동두천시", "과천시", "가평군", "양평군"
    };
    for (String r : moreRegions) {
      org.junit.jupiter.api.Assertions.assertTrue(body.contains(r), "MORE 지역 누락: " + r);
    }
    // 더보기 버튼 (primary 텍스트 링크)
    org.junit.jupiter.api.Assertions.assertTrue(
        body.contains("data-toggle-more"), "더보기 링크 누락");
    org.junit.jupiter.api.Assertions.assertTrue(
        body.contains("지역 더 보기"), "더보기 라벨 누락");
    // MORE 20 은 hidden 속성 부여됨
    org.junit.jupiter.api.Assertions.assertTrue(
        body.contains("welcome-toggle--more"), "MORE 클래스 누락");
    // 관심 분야 7종
    for (String c : UserInterestCategory.ALL) {
      org.junit.jupiter.api.Assertions.assertTrue(body.contains(c), "분야 누락: " + c);
    }
    // 체크 아이콘 SVG path
    org.junit.jupiter.api.Assertions.assertTrue(
        body.contains("M5 12l4 4 10-10"), "체크 SVG path 누락");
  }

  @Test
  void GET_welcome_noHeader_시각() throws Exception {
    // fragments/header 는 header-nav 클래스로 랜더됨. noHeader 대상이므로 부재해야 함.
    mockMvc
        .perform(get("/welcome").with(authed()))
        .andExpect(status().isOk())
        .andExpect(content().string(not(containsString("class=\"header-nav\""))));
  }

  @Test
  void POST_welcome_302_home_리다이렉트_toast() throws Exception {
    mockMvc
        .perform(
            post("/welcome")
                .with(authed())
                .with(csrf())
                .param("regions", "수원시", "성남시")
                .param("categories", "취업·역량"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?welcomed=personalized"));
  }

  @Test
  void POST_welcome_skip_302_home_리다이렉트() throws Exception {
    mockMvc
        .perform(post("/welcome/skip").with(authed()).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrl("/?welcomed=skip"));
  }
}
