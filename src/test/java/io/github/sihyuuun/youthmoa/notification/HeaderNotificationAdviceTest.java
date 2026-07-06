package io.github.sihyuuun.youthmoa.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

/**
 * F2 {@link HeaderNotificationAdvice} 단위 테스트.
 *
 * <p>Spring context 를 띄우지 않고 Advice 인스턴스를 직접 만들어 model 주입 로직만 검증. UserRepository /
 * NotificationService 는 Mockito 로 mock.
 */
class HeaderNotificationAdviceTest {

  @Test
  void 비인증_null_principal_시_기본값_주입() {
    NotificationService service = Mockito.mock(NotificationService.class);
    UserRepository userRepo = Mockito.mock(UserRepository.class);
    HeaderNotificationAdvice advice = new HeaderNotificationAdvice(service, userRepo);

    long count = advice.headerUnreadCount(null);
    List<Notification> recent = advice.headerRecentNotifications(null);

    assertThat(count).isZero();
    assertThat(recent).isEmpty();
    // repository / service 는 호출되지 않음
    Mockito.verifyNoInteractions(service, userRepo);
  }

  @Test
  void 인증_시_userRepository_와_service_호출_결과_반환() {
    NotificationService service = Mockito.mock(NotificationService.class);
    UserRepository userRepo = Mockito.mock(UserRepository.class);
    HeaderNotificationAdvice advice = new HeaderNotificationAdvice(service, userRepo);

    User user =
        User.builder().email("adv@t.com").password("x").name("헤더유저").role(UserRole.USER).build();
    UserPrincipal principal =
        new UserPrincipal(user) {
          @Override
          public Long getId() {
            return 42L;
          }
        };
    given(userRepo.findById(42L)).willReturn(Optional.of(user));
    given(service.unreadCount(user)).willReturn(3L);
    given(service.recentForHeader(user))
        .willReturn(
            List.of(
                Notification.builder()
                    .user(user)
                    .type(NotificationType.WELCOME)
                    .title("t")
                    .message("m")
                    .link("/l")
                    .build()));

    long count = advice.headerUnreadCount(principal);
    List<Notification> recent = advice.headerRecentNotifications(principal);

    assertThat(count).isEqualTo(3L);
    assertThat(recent).hasSize(1);
  }

  @Test
  void 인증_이지만_userRepository_miss_시_기본값() {
    NotificationService service = Mockito.mock(NotificationService.class);
    UserRepository userRepo = Mockito.mock(UserRepository.class);
    HeaderNotificationAdvice advice = new HeaderNotificationAdvice(service, userRepo);

    User user =
        User.builder().email("x@t.com").password("x").name("no").role(UserRole.USER).build();
    UserPrincipal principal =
        new UserPrincipal(user) {
          @Override
          public Long getId() {
            return 999L;
          }
        };
    given(userRepo.findById(999L)).willReturn(Optional.empty());

    assertThat(advice.headerUnreadCount(principal)).isZero();
    assertThat(advice.headerRecentNotifications(principal)).isEmpty();
    // service 는 호출되지 않음 (userRepository miss 시 orElse 경로)
    Mockito.verify(service, Mockito.never()).unreadCount(any());
  }

  @Test
  void 모델_주입_경로_통합_확인() {
    // @ModelAttribute("headerUnreadCount") / ("headerRecentNotifications") 는 Spring MVC 가 자동 호출.
    // 이 테스트는 model 에 직접 주입되는 값의 정합만 재확인 (실 MVC 통합은 PageRenderIntegrationTest 가 커버).
    NotificationService service = Mockito.mock(NotificationService.class);
    UserRepository userRepo = Mockito.mock(UserRepository.class);
    HeaderNotificationAdvice advice = new HeaderNotificationAdvice(service, userRepo);

    Model model = new ExtendedModelMap();
    model.addAttribute("headerUnreadCount", advice.headerUnreadCount(null));
    model.addAttribute("headerRecentNotifications", advice.headerRecentNotifications(null));

    assertThat(model.getAttribute("headerUnreadCount")).isEqualTo(0L);
    assertThat(model.getAttribute("headerRecentNotifications")).isEqualTo(List.of());
  }
}
