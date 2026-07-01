package io.github.sihyuuun.youthmoa.web;

import io.github.sihyuuun.youthmoa.common.SiteImage;
import io.github.sihyuuun.youthmoa.notice.Notice;
import io.github.sihyuuun.youthmoa.program.Program;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * F0e — 홈 컨트롤러 model attribute 완비 검증.
 *
 * 비로그인 시 topPrograms 채워지고 recommendedPrograms 는 빈 리스트.
 */
@WebMvcTest(HomeController.class)
@Import(HomeControllerTest.SecurityDisableConfig.class)
class HomeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean HomeService homeService;

    @Test
    @WithAnonymousUser
    void anonymousUser_showsTopPrograms() throws Exception {
        given(homeService.getHeroImageUrl()).willReturn("https://example.com/hero.jpg");
        given(homeService.countActivePrograms()).willReturn(5L);
        given(homeService.countCenters()).willReturn(48L);
        given(homeService.countTotalApplicants()).willReturn(120L);
        given(homeService.findTopPrograms()).willReturn(List.<Program>of());
        // Spring Model.addAttribute(name, null) 은 값을 저장 안 함 → attributeExists 통과 위해 non-null mock
        given(homeService.findMainNotice()).willReturn(
                Notice.builder().title("mock").content("mock").tag("공지").isPinned(true).build());
        given(homeService.findSubNotices()).willReturn(List.<Notice>of());
        given(homeService.findSpaceImages()).willReturn(List.<SiteImage>of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists(
                        "currentPage", "heroImageUrl",
                        "activeProgramCount", "centerCount", "totalApplicantCount",
                        "topPrograms", "recommendedPrograms",
                        "mainNotice", "subNotices",
                        "spaceImages", "spaceLabels"
                ))
                .andExpect(model().attribute("activeProgramCount", 5L))
                .andExpect(model().attribute("centerCount", 48L))
                .andExpect(model().attribute("totalApplicantCount", 120L));
    }

    /** SecurityConfig 를 제외하고 익명 접근 허용. */
    @org.springframework.boot.test.context.TestConfiguration
    static class SecurityDisableConfig {
        @org.springframework.context.annotation.Bean
        org.springframework.security.web.SecurityFilterChain filter(
                org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
            http.csrf(cs -> cs.disable())
                .authorizeHttpRequests(a -> a.anyRequest().permitAll());
            return http.build();
        }
    }
}
