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
  void GET_welcome_로그인_200_지역12_분야7_SVG_렌더() throws Exception {
    String body =
        mockMvc
            .perform(get("/welcome").with(authed()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    // 관심 지역 12개 — prototype 명시 12건 모두 렌더
    String[] regions = {
      "수원시", "성남시", "고양시", "용인시", "부천시", "안양시", "안산시", "화성시", "남양주시", "평택시", "의정부시", "시흥시"
    };
    for (String r : regions) {
      org.junit.jupiter.api.Assertions.assertTrue(body.contains(r), "지역 누락: " + r);
    }
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
