package io.github.sihyuuun.youthmoa.notification;

import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 페이지 렌더 시 헤더 알림 뱃지·드롭다운용 데이터를 모델에 자동 주입.
 *
 * <p>{@code @ControllerAdvice(annotations = Controller.class)} — {@code @Controller} 붙은 컨트롤러 전체에
 * 적용, {@code @RestController} 는 자동 제외. {@code @ModelAttribute} 는 각 요청마다 실행되어 리턴값을 지정된 이름으로 model 에
 * 추가.
 *
 * <p>모델 attribute 예약어(application/session/request 등) 회피 — {@code headerUnreadCount}, {@code
 * headerRecentNotifications} 로 명명 (CLAUDE.md Thymeleaf 규칙).
 */
@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class HeaderNotificationAdvice {

  private final NotificationService notificationService;
  private final UserRepository userRepository;

  @ModelAttribute("headerUnreadCount")
  @Transactional(readOnly = true)
  public long headerUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
    if (principal == null) {
      return 0L;
    }
    return userRepository
        .findById(principal.getId())
        .map(notificationService::unreadCount)
        .orElse(0L);
  }

  @ModelAttribute("headerRecentNotifications")
  @Transactional(readOnly = true)
  public List<Notification> headerRecentNotifications(
      @AuthenticationPrincipal UserPrincipal principal) {
    if (principal == null) {
      return List.of();
    }
    return userRepository
        .findById(principal.getId())
        .map(notificationService::recentForHeader)
        .orElse(List.of());
  }
}
