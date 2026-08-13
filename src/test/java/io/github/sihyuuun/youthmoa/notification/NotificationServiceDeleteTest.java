package io.github.sihyuuun.youthmoa.notification;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Q-3 (2026-08-13) 개별 알림 삭제 서비스 계층 검증.
 *
 * <p>NotificationServiceDeleteTest — 소유자 검증(다른 유저 알림 접근 시 404), 존재하지 않는 id 시 404, 정상 삭제 시 Repository.delete
 * 위임 확인. hard delete 라 soft-delete 컬럼은 검증 대상 아님.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceDeleteTest {

  @Mock NotificationRepository notificationRepository;
  @Mock UserRepository userRepository;

  @InjectMocks NotificationService notificationService;

  private User owner;
  private User attacker;

  @BeforeEach
  void setUp() throws Exception {
    owner = User.builder().email("owner@t").password("x").name("owner").role(UserRole.USER).build();
    setId(owner, 7L);
    attacker = User.builder().email("atk@t").password("x").name("atk").role(UserRole.USER).build();
    setId(attacker, 999L);
  }

  @Test
  void 정상_삭제_소유자_일치() {
    Notification n =
        Notification.builder()
            .user(owner)
            .type(NotificationType.WELCOME)
            .title("t")
            .message("m")
            .build();
    given(notificationRepository.findById(11L)).willReturn(Optional.of(n));

    notificationService.delete(11L, 7L);

    verify(notificationRepository).delete(n);
  }

  @Test
  void 존재하지_않는_id_는_404() {
    given(notificationRepository.findById(9999L)).willReturn(Optional.empty());

    assertThatThrownBy(() -> notificationService.delete(9999L, 7L))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);

    verify(notificationRepository, never()).delete(any());
  }

  @Test
  void 다른_유저_알림_삭제_시도는_404_권한_이탈_방어() {
    Notification othersNotif =
        Notification.builder()
            .user(owner)
            .type(NotificationType.WELCOME)
            .title("t")
            .message("m")
            .build();
    given(notificationRepository.findById(11L)).willReturn(Optional.of(othersNotif));

    // attacker(id=999) 가 owner(id=7) 의 알림을 삭제 시도 → 404 (403 대신 존재 은닉)
    assertThatThrownBy(() -> notificationService.delete(11L, 999L))
        .isInstanceOf(ResponseStatusException.class)
        .extracting(e -> ((ResponseStatusException) e).getStatusCode())
        .isEqualTo(HttpStatus.NOT_FOUND);

    verify(notificationRepository, never()).delete(any());
  }

  private static void setId(User u, Long id) throws Exception {
    Field f = User.class.getDeclaredField("id");
    f.setAccessible(true);
    f.set(u, id);
  }
}
