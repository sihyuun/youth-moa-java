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
 * 260826 A#2: 마이페이지 프로필 편집에서 phone 변경 시 phoneVerified 리셋 회귀 방어.
 *
 * <p>배경: F-signup-01 (CoolSMS SMS 인증) 은 signup 흐름에서만 phoneVerified 를 관리. 마이페이지에서 phone 만 바뀌면 이전 인증
 * 상태가 그대로 남아 다른 번호로 인증한 것처럼 보이는 데이터 무결성 취약점.
 *
 * <p>조치: {@code UserService.updateProfile} 에서 request.phone 이 currentUser.phone 과 다르면 {@code
 * user.resetPhoneVerified()} 호출.
 */
@ExtendWith(MockitoExtension.class)
class UserServicePhoneVerifiedResetTest {

  @Mock UserRepository userRepository;
  @Mock org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
  @Mock io.github.sihyuuun.youthmoa.application.ApplicationRepository applicationRepository;
  @Mock io.github.sihyuuun.youthmoa.bookmark.BookmarkRepository bookmarkRepository;
  @Mock io.github.sihyuuun.youthmoa.notification.NotificationRepository notificationRepository;

  @InjectMocks UserService userService;

  private User seedVerifiedUser() {
    return User.builder()
        .email("verified@youth-moa.test")
        .password("x")
        .name("초기이름")
        .phone("01011112222")
        .phoneVerified(true) // signup 흐름에서 이미 인증 완료된 상태 시뮬레이션
        .build();
  }

  private ProfileUpdateRequest request(String phone) {
    ProfileUpdateRequest r = new ProfileUpdateRequest();
    r.setName("이름");
    r.setPhone(phone);
    // 비밀번호 · 관심 · 성별 등은 기본값 유지 (updateProfile 다른 시나리오와 무관)
    return r;
  }

  @Test
  @DisplayName("phone 변경 시 phoneVerified 가 false 로 리셋된다")
  void updateProfile_phone_changed_resets_verified() {
    User user = seedVerifiedUser();
    given(userRepository.findByEmail(any())).willReturn(Optional.of(user));

    userService.updateProfile("verified@youth-moa.test", request("01099998888"));

    assertThat(user.isPhoneVerified()).isFalse();
    assertThat(user.getPhone()).isEqualTo("01099998888");
  }

  @Test
  @DisplayName("phone 동일 유지 시 phoneVerified 는 그대로 true")
  void updateProfile_phone_unchanged_keeps_verified() {
    User user = seedVerifiedUser();
    given(userRepository.findByEmail(any())).willReturn(Optional.of(user));

    userService.updateProfile("verified@youth-moa.test", request("01011112222"));

    assertThat(user.isPhoneVerified()).isTrue();
  }

  @Test
  @DisplayName("request.phone 이 null 이면 리셋 안 함 (안전망)")
  void updateProfile_null_phone_no_reset() {
    User user = seedVerifiedUser();
    given(userRepository.findByEmail(any())).willReturn(Optional.of(user));

    ProfileUpdateRequest r = request(null);
    userService.updateProfile("verified@youth-moa.test", r);

    assertThat(user.isPhoneVerified()).isTrue();
  }
}
