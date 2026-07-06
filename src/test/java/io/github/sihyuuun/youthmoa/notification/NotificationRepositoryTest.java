package io.github.sihyuuun.youthmoa.notification;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * F2 Repository 쿼리 검증 (@DataJpaTest H2).
 *
 * <p>JpaConfig import 필수 — @DataJpaTest 는 기본으로 @Configuration 을 스캔하지 않아 @EnableJpaAuditing 미로드
 * → @CreatedDate null.
 */
@DataJpaTest
@Import(JpaConfig.class)
class NotificationRepositoryTest {

  @Autowired NotificationRepository notificationRepository;
  @Autowired UserRepository userRepository;

  private User user;
  private User other;

  @BeforeEach
  void setUp() {
    user =
        userRepository.save(
            User.builder()
                .email("f2-repo-user@test.com")
                .password("hashed")
                .name("알림받는이")
                .role(UserRole.USER)
                .build());
    other =
        userRepository.save(
            User.builder()
                .email("f2-repo-other@test.com")
                .password("hashed")
                .name("타인")
                .role(UserRole.USER)
                .build());
  }

  @Test
  @DisplayName("findTop5ByUserOrderByCreatedAtDesc — user 기준 최근 5건, 다른 유저 알림 제외")
  void findTop5_orders_by_recency_and_filters_by_user() throws InterruptedException {
    // 다른 유저 알림 (필터되어야 함)
    save(other, NotificationType.WELCOME, "타인 알림");

    // 대상 유저에 6건 저장 (createdAt 순서를 위해 짧게 sleep)
    for (int i = 1; i <= 6; i++) {
      save(user, NotificationType.WELCOME, "알림 " + i);
      Thread.sleep(2); // @CreatedDate ordering 안정화
    }

    List<Notification> top5 = notificationRepository.findTop5ByUserOrderByCreatedAtDesc(user);
    assertThat(top5).hasSize(5);
    // 최근 생성된 (알림 6, 5, 4, 3, 2) 순
    assertThat(top5.get(0).getTitle()).isEqualTo("알림 6");
    assertThat(top5.get(4).getTitle()).isEqualTo("알림 2");
    // 다른 유저 알림 없음
    assertThat(top5).allMatch(n -> n.getUser().getId().equals(user.getId()));
  }

  @Test
  @DisplayName("countByUserAndIsReadFalse — read=true 인 알림 제외")
  void countByUserAndIsReadFalse_excludes_read() {
    Notification unread1 = save(user, NotificationType.WELCOME, "unread 1");
    save(user, NotificationType.WELCOME, "unread 2");
    Notification read1 = save(user, NotificationType.WELCOME, "read");
    read1.markAsRead();
    notificationRepository.save(read1);
    // 다른 유저 unread — count 에 포함되면 안 됨
    save(other, NotificationType.WELCOME, "타인 unread");

    long count = notificationRepository.countByUserAndIsReadFalse(user);
    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("markAllAsRead — 대상 유저의 unread 만 read 로 갱신, 다른 유저는 그대로")
  void markAllAsRead_only_target_user() {
    save(user, NotificationType.WELCOME, "u1");
    save(user, NotificationType.WELCOME, "u2");
    Notification otherUnread = save(other, NotificationType.WELCOME, "other-unread");

    int updated = notificationRepository.markAllAsRead(user);
    assertThat(updated).isEqualTo(2);

    // Flush 후 확인 필요 (Modifying 은 JPA context flush 요구)
    notificationRepository.flush();

    assertThat(notificationRepository.countByUserAndIsReadFalse(user)).isZero();
    // 다른 유저는 여전히 unread
    assertThat(notificationRepository.countByUserAndIsReadFalse(other)).isEqualTo(1);
  }

  private Notification save(User u, NotificationType type, String title) {
    return notificationRepository.save(
        Notification.builder()
            .user(u)
            .type(type)
            .title(title)
            .message("message")
            .link("/link")
            .build());
  }
}
