package io.github.sihyuuun.youthmoa.notification;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D1b: NotificationChannelResolver 단위 테스트.
 *
 * <p>채널 활성 조합 4가지 (3/2/1/0) 케이스와 enum 순서 보존을 검증.
 */
class NotificationChannelResolverTest {

  private final NotificationChannelResolver resolver = new NotificationChannelResolver();

  private User buildUser(boolean kakao, boolean sms, boolean email) {
    return User.builder()
        .email("t@t.t")
        .name("t")
        .role(UserRole.USER)
        .notifyKakao(kakao)
        .notifySms(sms)
        .notifyEmail(email)
        .build();
  }

  @Test
  @DisplayName("3채널 모두 활성이면 KAKAO → SMS → EMAIL 순서로 반환한다")
  void all_three() {
    List<NotificationChannel> ch = resolver.activeChannelsFor(buildUser(true, true, true));
    assertThat(ch)
        .containsExactly(
            NotificationChannel.KAKAO, NotificationChannel.SMS, NotificationChannel.EMAIL);
  }

  @Test
  @DisplayName("2채널 (기본값: 카카오+이메일) 활성이면 KAKAO, EMAIL 순서로 반환한다")
  void two_default() {
    List<NotificationChannel> ch = resolver.activeChannelsFor(buildUser(true, false, true));
    assertThat(ch).containsExactly(NotificationChannel.KAKAO, NotificationChannel.EMAIL);
  }

  @Test
  @DisplayName("1채널만 활성이면 해당 채널만 반환한다")
  void one_only() {
    List<NotificationChannel> ch = resolver.activeChannelsFor(buildUser(false, false, true));
    assertThat(ch).containsExactly(NotificationChannel.EMAIL);
  }

  @Test
  @DisplayName("0채널이면 빈 리스트를 반환한다")
  void none() {
    List<NotificationChannel> ch = resolver.activeChannelsFor(buildUser(false, false, false));
    assertThat(ch).isEmpty();
  }
}
