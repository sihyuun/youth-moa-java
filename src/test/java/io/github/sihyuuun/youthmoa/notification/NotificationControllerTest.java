package io.github.sihyuuun.youthmoa.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** F2 NotificationController — GET /notifications + POST /read-all + POST /{id}/read. */
@WebMvcTest(NotificationController.class)
@Import(NotificationControllerTest.SecurityDisableConfig.class)
class NotificationControllerTest {

  @Autowired MockMvc mockMvc;
  @MockitoBean NotificationService notificationService;
  @MockitoBean UserRepository userRepository;

  private User seed;
  private UserPrincipal principal;

  @BeforeEach
  void setUp() throws Exception {
    seed = User.builder().email("nc@t.com").password("x").name("알림유저").role(UserRole.USER).build();
    Field idf = User.class.getDeclaredField("id");
    idf.setAccessible(true);
    idf.set(seed, 7L);
    principal = new UserPrincipal(seed);
    given(userRepository.findById(7L)).willReturn(Optional.of(seed));
  }

  @Test
  void GET_notifications_인증시_stub_페이지_렌더() throws Exception {
    given(notificationService.listAll(seed)).willReturn(List.of());
    mockMvc
        .perform(get("/notifications").with(user(principal)))
        .andExpect(status().isOk())
        .andExpect(view().name("notification/list"))
        .andExpect(model().attributeExists("notifications"));
  }

  @Test
  void POST_read_all_HX요청은_panel_fragment_반환() throws Exception {
    given(notificationService.unreadCount(seed)).willReturn(0L);
    given(notificationService.recentForHeader(seed)).willReturn(List.of());

    mockMvc
        .perform(
            post("/notifications/read-all")
                .header("HX-Request", "true")
                .with(user(principal))
                .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("fragments/notification-panel :: panel"))
        .andExpect(model().attribute("headerUnreadCount", 0L));

    // Service 호출 여부
    org.mockito.Mockito.verify(notificationService).markAllAsRead(7L);
  }

  @Test
  void POST_read_all_일반POST는_notifications로_리다이렉트() throws Exception {
    // 전체 페이지 폼 제출 시나리오 — HX-Request 헤더 없음
    mockMvc
        .perform(post("/notifications/read-all").with(user(principal)).with(csrf()))
        .andExpect(status().is3xxRedirection())
        .andExpect(view().name("redirect:/notifications"));

    org.mockito.Mockito.verify(notificationService).markAllAsRead(7L);
  }

  @Test
  void POST_id_read_개별_읽음_처리후_redirectLink_주입() throws Exception {
    Notification n =
        Notification.builder()
            .user(seed)
            .type(NotificationType.APPLICATION_APPROVED)
            .title("t")
            .message("m")
            .link("/apply/complete?applicationId=1")
            .build();
    given(notificationService.markAsRead(11L, 7L)).willReturn(n);
    given(notificationService.unreadCount(seed)).willReturn(3L);
    given(notificationService.recentForHeader(seed)).willReturn(List.of(n));

    mockMvc
        .perform(post("/notifications/11/read").with(user(principal)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(view().name("fragments/notification-panel :: panel"))
        .andExpect(model().attribute("redirectLink", "/apply/complete?applicationId=1"));
  }

  @Test
  void POST_id_read_link_null_이면_기본_notifications_경로() throws Exception {
    Notification n =
        Notification.builder()
            .user(seed)
            .type(NotificationType.WELCOME)
            .title("t")
            .message("m")
            .link(null)
            .build();
    given(notificationService.markAsRead(any(), any())).willReturn(n);
    given(notificationService.unreadCount(seed)).willReturn(0L);
    given(notificationService.recentForHeader(seed)).willReturn(List.of());

    mockMvc
        .perform(post("/notifications/22/read").with(user(principal)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(model().attribute("redirectLink", "/notifications"));
  }

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
