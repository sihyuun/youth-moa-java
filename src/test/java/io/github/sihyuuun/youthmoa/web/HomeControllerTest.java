package io.github.sihyuuun.youthmoa.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import io.github.sihyuuun.youthmoa.common.SiteImage;
import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.program.Program;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * F0e — 홈 컨트롤러 model attribute 완비 검증.
 *
 * <p>비로그인 시 topPrograms 채워지고 recommendedPrograms 는 빈 리스트.
 */
@WebMvcTest(HomeController.class)
@Import(HomeControllerTest.SecurityDisableConfig.class)
class HomeControllerTest {

  @Autowired MockMvc mockMvc;

  @MockitoBean HomeService homeService;

  // 2026-07-29 fix/home-contract-gaps: HomeController 가 즐겨찾기 별·CTA 렌더용으로 BookmarkService 를 주입받음
  @MockitoBean io.github.sihyuuun.youthmoa.bookmark.BookmarkService bookmarkService;

  // HeaderNotificationAdvice 의존성 mock (F2 @ControllerAdvice 도입 후 필요)
  @MockitoBean io.github.sihyuuun.youthmoa.notification.NotificationService notificationService;

  @MockitoBean io.github.sihyuuun.youthmoa.user.UserRepository userRepository;

  @Test
  @WithAnonymousUser
  void anonymousUser_showsTopPrograms() throws Exception {
    given(homeService.getHeroImageUrls())
        .willReturn(List.of("https://example.com/hero1.jpg", "https://example.com/hero2.jpg"));
    given(homeService.countActivePrograms()).willReturn(5L);
    given(homeService.countCenters()).willReturn(48L);
    given(homeService.countTotalApplicants()).willReturn(120L);
    given(homeService.findTopPrograms()).willReturn(List.<Program>of());
    // Spring Model.addAttribute(name, null) 은 값을 저장 안 함 → attributeExists 통과 위해 non-null mock
    given(homeService.findMainNotice())
        .willReturn(
            Notice.builder()
                .title("mock")
                .content("mock")
                .category(io.github.sihyuuun.youthmoa.notice.NoticeCategory.NOTICE)
                .isPinned(true)
                .build());
    given(homeService.findSubNotices()).willReturn(List.<Notice>of());
    given(homeService.findSpaceImages()).willReturn(List.<SiteImage>of());

    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(
            model()
                .attributeExists(
                    "currentPage",
                    "heroImageUrls",
                    "activeProgramCount",
                    "centerCount",
                    "totalApplicantCount",
                    "topPrograms",
                    "recommendedPrograms",
                    "mainNotice",
                    "subNotices",
                    "spaceImages",
                    "spaceLabels"))
        .andExpect(model().attribute("activeProgramCount", 5L))
        .andExpect(model().attribute("centerCount", 48L))
        .andExpect(model().attribute("totalApplicantCount", 120L));
  }

  /** SecurityConfig 를 제외하고 익명 접근 허용. */
  @org.springframework.boot.test.context.TestConfiguration
  static class SecurityDisableConfig {
    @org.springframework.context.annotation.Bean
    org.springframework.security.web.SecurityFilterChain filter(
        org.springframework.security.config.annotation.web.builders.HttpSecurity http)
        throws Exception {
      http.csrf(cs -> cs.disable()).authorizeHttpRequests(a -> a.anyRequest().permitAll());
      return http.build();
    }
  }
}
