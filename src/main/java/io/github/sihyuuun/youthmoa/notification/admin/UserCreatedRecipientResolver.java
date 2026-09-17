package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import io.github.sihyuuun.youthmoa.user.event.UserCreatedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * A7 admin 헤더 알림 벨 QE (2026-09-17): NEW_USER 수신자 결정 전략.
 *
 * <p>SYSTEM_ADMIN (활성) 전원 대상. 회원가입 시점엔 사용자에게 {@code center} 배정이 없으므로 CENTER_ADMIN 은 자기 센터 사용자인지 판별할
 * 근거가 없다 → 제외.
 */
@Component
@RequiredArgsConstructor
public class UserCreatedRecipientResolver
    implements NotificationRecipientResolver<UserCreatedEvent> {

  private final UserRepository userRepository;

  @Override
  public List<User> resolve(UserCreatedEvent event) {
    return userRepository.findByRoleAndIsActiveTrue(UserRole.SYSTEM_ADMIN);
  }
}
