package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.notification.NotificationService;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * A7 admin 헤더 알림 벨 (2026-09-17): 관리자 페이지 렌더 시 헤더 배지 초기값을 모델에 자동 주입.
 *
 * <p>사용자 트랙의 {@code HeaderNotificationAdvice} 와 완전 분리된 admin advice. 두 advice 가 모두 실행되어도 model
 * attribute 이름이 다르므로({@code headerUnreadCount} vs {@code adminHeaderUnreadCount}) 충돌하지 않는다.
 *
 * <p>Qn-D polling (30s) 이 배지를 이후 갱신하므로 여기서는 **최초 렌더 값만** 계산한다. 인증되지 않았거나 USER role 이면 0.
 *
 * <p>주의: 의존성은 사용자 트랙과 공유하는 {@link NotificationService} · {@link UserRepository} 만 사용한다. admin-only
 * bean ({@code AdminNotificationService}) 을 참조하면 사용자 트랙 {@code @WebMvcTest} (예: {@code
 * NotificationControllerTest}) 가 이 advice bean 을 함께 로드하면서 미주입 실패를 낸다. 사용자 트랙 회귀 방어를 위해 공용 bean 만
 * 사용.
 *
 * <p>모델 attribute 이름 예약어(application/session/request) 회피 — {@code adminHeaderUnreadCount}로 명명
 * (CLAUDE.md Thymeleaf 규칙).
 */
@ControllerAdvice(
    basePackages = {
      "io.github.sihyuuun.youthmoa.admin",
      "io.github.sihyuuun.youthmoa.notification.admin"
    })
@RequiredArgsConstructor
public class AdminHeaderNotificationAdvice {

  private final NotificationService notificationService;
  private final UserRepository userRepository;

  @ModelAttribute("adminHeaderUnreadCount")
  @Transactional(readOnly = true)
  public long adminHeaderUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
    if (principal == null) {
      return 0L;
    }
    if (!isAdmin()) {
      return 0L;
    }
    return userRepository
        .findById(principal.getId())
        .map(notificationService::unreadCount)
        .orElse(0L);
  }

  /** 현재 인증 컨텍스트에서 관리자(SYSTEM_ADMIN 또는 CENTER_ADMIN) 여부 확인. authority 는 role prefix "ROLE_" 를 포함. */
  private boolean isAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return false;
    return auth.getAuthorities().stream()
        .anyMatch(
            a ->
                "ROLE_SYSTEM_ADMIN".equals(a.getAuthority())
                    || "ROLE_CENTER_ADMIN".equals(a.getAuthority()));
  }
}
