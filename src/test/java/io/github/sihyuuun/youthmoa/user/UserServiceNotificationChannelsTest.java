package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * D5 알림 채널·항목 저장 UserService 단위 테스트.
 *
 * <p>회귀 방어 핵심: {@code NotificationChannelRequest} 의 항목 3필드(remindD1/waitlistEmpty/newProgramNews)
 * 를 부분 전송(누락)해도 기존 값이 파괴되지 않아야 한다 (Boolean nullable 계약).
 */
@ExtendWith(MockitoExtension.class)
class UserServiceNotificationChannelsTest {

  @Mock UserRepository userRepository;
  @Mock org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
  @Mock io.github.sihyuuun.youthmoa.application.ApplicationRepository applicationRepository;
  @Mock io.github.sihyuuun.youthmoa.bookmark.BookmarkRepository bookmarkRepository;
  @Mock io.github.sihyuuun.youthmoa.notification.NotificationRepository notificationRepository;

  @InjectMocks UserService userService;

  private User seedUser() {
    // 초기 상태: 항목 3개 default (true, true, false) — User 필드 기본값 반영
    return User.builder()
        .email("t@youth-moa.test")
        .password("x")
        .name("테스터")
        .phone("01000000000")
        .notifyKakao(true)
        .notifySms(false)
        .notifyEmail(true)
        .build();
  }

  @Test
  @DisplayName("항목 필드 3개를 request 에 담아 보내면 그 값으로 저장")
  void 항목_필드_전체_저장() {
    User u = seedUser();
    given(userRepository.findByEmail(any())).willReturn(Optional.of(u));

    NotificationChannelRequest req = new NotificationChannelRequest();
    req.setKakao(true);
    req.setSms(true);
    req.setEmail(false);
    req.setRemindD1(false);
    req.setWaitlistEmpty(false);
    req.setNewProgramNews(true);

    userService.updateNotificationChannels("t@youth-moa.test", req);

    assertThat(u.isNotifyKakao()).isTrue();
    assertThat(u.isNotifySms()).isTrue();
    assertThat(u.isNotifyEmail()).isFalse();
    assertThat(u.isNotifyRemindD1()).isFalse();
    assertThat(u.isNotifyWaitlistEmpty()).isFalse();
    assertThat(u.isNotifyNewProgramNews()).isTrue();
  }

  @Test
  @DisplayName("항목 필드 3개를 request 에서 누락하면 기존값 유지 (Boolean nullable 계약)")
  void 항목_필드_누락시_기존값_유지() {
    User u = seedUser();
    // 시나리오: 사용자가 이전에 remindD1=true(default) / waitlistEmpty=true(default) / newProgramNews=false 로 저장됨.
    given(userRepository.findByEmail(any())).willReturn(Optional.of(u));

    NotificationChannelRequest req = new NotificationChannelRequest();
    // 채널만 세팅. 항목 3개는 null 로 두어 부분 저장 시뮬레이션.
    req.setKakao(false);
    req.setSms(false);
    req.setEmail(false);

    userService.updateNotificationChannels("t@youth-moa.test", req);

    // 채널은 요청대로 반영
    assertThat(u.isNotifyKakao()).isFalse();
    assertThat(u.isNotifySms()).isFalse();
    assertThat(u.isNotifyEmail()).isFalse();
    // 항목은 default 유지되어야 함 (파괴 방어)
    assertThat(u.isNotifyRemindD1()).isTrue();
    assertThat(u.isNotifyWaitlistEmpty()).isTrue();
    assertThat(u.isNotifyNewProgramNews()).isFalse();
  }
}
