package io.github.sihyuuun.youthmoa.notification.admin;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.notification.Notification;
import io.github.sihyuuun.youthmoa.notification.NotificationRepository;
import io.github.sihyuuun.youthmoa.notification.NotificationType;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * A7 admin 헤더 알림 벨 (2026-09-17): admin 드롭다운 · 배지 fragment 실 렌더 검증.
 *
 * <p>MockMvc 로 실제 Thymeleaf 렌더까지 수행해 fragment 표현식({@code th:*}) 이 응답 HTML 에 잔존하지 않는지 확인. F0h-c2 회고
 * 규칙 — compileJava 만으로는 fragment 파싱 실패를 감지하지 못한다.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AdminNotificationRenderTest {

  @Autowired WebApplicationContext webApplicationContext;
  @Autowired UserRepository userRepository;
  @Autowired NotificationRepository notificationRepository;

  private MockMvc mockMvc;
  private User admin;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    admin =
        userRepository.save(
            User.builder()
                .email("a7-render-admin@test.com")
                .password("hashed")
                .name("관리자")
                .role(UserRole.SYSTEM_ADMIN)
                .build());
    UserPrincipal principal = new UserPrincipal(admin);
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "n/a", principal.getAuthorities()));

    // 알림 2건 시드 — 하나는 unread, 하나는 read.
    notificationRepository.save(
        Notification.builder()
            .user(admin)
            .type(NotificationType.NEW_APPLICATION)
            .title("새 신청이 접수됐어요")
            .message("A7 프로그램에 새 신청이 들어왔어요.")
            .link("/admin/programs/1/applications")
            .build());
    Notification readOne =
        notificationRepository.save(
            Notification.builder()
                .user(admin)
                .type(NotificationType.NEW_USER)
                .title("새 회원이 가입했어요")
                .message("홍길동(a@b.com) 님이 회원가입 했어요.")
                .link("/admin/users/99")
                .build());
    readOne.markAsRead();
    notificationRepository.save(readOne);
  }

  @Test
  @DisplayName("GET /admin/notifications/dropdown — 200 OK + panel 마크업 + Thymeleaf 표현식 잔존 0")
  void dropdown_renders_without_leftover_expressions() throws Exception {
    String html =
        mockMvc
            .perform(get("/admin/notifications/dropdown"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(html).contains("admin-notif-panel");
    assertThat(html).contains("admin-notif-item");
    assertThat(html).contains("새 신청이 접수됐어요");
    // Thymeleaf 잔존 검증 — 표현식이 미평가로 남으면 실패.
    assertThat(html).doesNotContain("${").doesNotContain("th:each");
  }

  @Test
  @DisplayName("GET /admin/notifications/badge — 200 OK + #admin-notif-badge 포함 (unread=1 상태)")
  void badge_renders_unread_count() throws Exception {
    String html =
        mockMvc
            .perform(get("/admin/notifications/badge"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(html).contains("id=\"admin-notif-badge\"");
    assertThat(html).contains("data-notif-badge");
    // 시드 unread 1건 → 배지 텍스트 "1" 표기
    assertThat(html).contains(">1<");
    assertThat(html).doesNotContain("${");
    // 2026-09-17 P0 회귀 방어: fragment 정의 중복으로 <span id="admin-notif-badge"> 가
    // 2회 렌더되던 사고. id 는 응답 HTML 안에 정확히 1회만 등장해야 한다.
    int idOccurrences = html.split("id=\"admin-notif-badge\"", -1).length - 1;
    assertThat(idOccurrences)
        .as("#admin-notif-badge id 는 응답 안에 정확히 1개만 있어야 한다 (fragment 이름 충돌 회귀 방어)")
        .isEqualTo(1);
  }

  private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder get(
      String path) {
    return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path);
  }
}
