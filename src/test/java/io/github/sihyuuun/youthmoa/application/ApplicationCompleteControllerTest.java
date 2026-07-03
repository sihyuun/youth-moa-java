package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.notification.NotificationChannel;
import io.github.sihyuuun.youthmoa.notification.NotificationChannelResolver;
import io.github.sihyuuun.youthmoa.program.Program;
import io.github.sihyuuun.youthmoa.program.ProgramService;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * D1b: 신청 완료 페이지 컨트롤러 검증.
 * <ul>
 *   <li>정상: 200 + view + model 확인</li>
 *   <li>권한 위반: 다른 유저의 applicationId → 404 (403 X, 존재 노출 방지)</li>
 * </ul>
 */
@WebMvcTest(ApplicationController.class)
@Import(ApplicationCompleteControllerTest.SecurityDisableConfig.class)
class ApplicationCompleteControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ApplicationService applicationService;
    @MockitoBean ApplicationRepository applicationRepository;
    @MockitoBean ProgramService programService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean NotificationChannelResolver notificationChannelResolver;

    private User owner;
    private User other;
    private Program program;
    private Application application;

    @BeforeEach
    void setup() throws Exception {
        owner = User.builder().email("owner@t.com").name("주인").role(UserRole.USER).build();
        setId(owner, 1L);
        other = User.builder().email("other@t.com").name("남").role(UserRole.USER).build();
        setId(other, 2L);
        program = Program.builder()
                .title("테스트 프로그램").organization("센터").region("수원시")
                .content("c").requirements("r")
                .startDate(LocalDate.of(2024, 7, 1)).endDate(LocalDate.of(2024, 7, 31))
                .build();
        setId(program, 10L);
        application = Application.builder().user(owner).program(program).build();
        setId(application, 123L);
        // appliedAt 은 @CreatedDate 라 수동 셋
        Field f = Application.class.getDeclaredField("appliedAt");
        f.setAccessible(true);
        f.set(application, LocalDateTime.of(2024, 7, 5, 17, 11));
    }

    private static void setId(Object entity, Long id) throws Exception {
        Field f = entity.getClass().getDeclaredField("id");
        f.setAccessible(true);
        f.set(entity, id);
    }

    @Test
    @WithMockUser(username = "owner@t.com")
    void success_rendersComplete() throws Exception {
        given(userRepository.findByEmail("owner@t.com")).willReturn(Optional.of(owner));
        given(applicationRepository.findWithProgramAndUserById(123L)).willReturn(Optional.of(application));
        given(notificationChannelResolver.activeChannelsFor(owner))
                .willReturn(List.of(NotificationChannel.KAKAO, NotificationChannel.EMAIL));

        mockMvc.perform(get("/apply/complete").param("applicationId", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("application/complete"))
                .andExpect(model().attributeExists("myApplication", "program", "channels", "channelSubtitle"))
                .andExpect(model().attribute("channelSubtitle", "결과는 카카오톡·이메일로 안내드려요"));
    }

    @Test
    @WithMockUser(username = "other@t.com")
    void otherUser_returns404() throws Exception {
        given(userRepository.findByEmail("other@t.com")).willReturn(Optional.of(other));
        given(applicationRepository.findWithProgramAndUserById(123L)).willReturn(Optional.of(application));

        mockMvc.perform(get("/apply/complete").param("applicationId", "123"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "owner@t.com")
    void missingApplication_returns404() throws Exception {
        given(userRepository.findByEmail("owner@t.com")).willReturn(Optional.of(owner));
        given(applicationRepository.findWithProgramAndUserById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/apply/complete").param("applicationId", "999"))
                .andExpect(status().isNotFound());
    }

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
