package io.github.sihyuuun.youthmoa.notification.admin;

import io.github.sihyuuun.youthmoa.notification.Notification;
import io.github.sihyuuun.youthmoa.notification.NotificationService;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A7 admin 헤더 알림 벨 (2026-09-17): admin 트랙 알림 조회·조작 서비스.
 *
 * <p>Notification 엔티티는 {@code user} FK 하나로 사용자·관리자 알림을 공유(fan-out) 하므로 실제 조회는 사용자 트랙 {@link
 * NotificationService} 의 메서드를 그대로 위임한다. 이 서비스는 admin controller 의 편의 파사드 + 미래 확장(예: admin 전용 필터,
 * center-scope 검증) 지점 역할.
 *
 * <p>@PreAuthorize 는 컨트롤러 레벨에서 걸며 서비스는 principal id 만 신뢰한다 (Security 이중 검증 불필요).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminNotificationService {

  private final NotificationService notificationService;
  private final UserRepository userRepository;

  /** Qn-2: 헤더 드롭다운 최근 5건. 사용자 트랙과 동일 쿼리 재사용. */
  public List<Notification> recentForHeader(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new IllegalStateException("Authenticated admin not found: " + userId));
    return notificationService.recentForHeader(user);
  }

  /** Qn-6: 배지용 미읽음 count. */
  public long unreadCount(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(
                () -> new IllegalStateException("Authenticated admin not found: " + userId));
    return notificationService.unreadCount(user);
  }

  /** 개별 읽음 처리. NotificationService 가 소유자 검증 (다른 유저 알림 → 404). */
  public Notification markAsRead(Long notificationId, Long userId) {
    return notificationService.markAsRead(notificationId, userId);
  }

  /** 모두 읽음. */
  public int markAllAsRead(Long userId) {
    return notificationService.markAllAsRead(userId);
  }

  /** 개별 삭제. */
  public void delete(Long notificationId, Long userId) {
    notificationService.delete(notificationId, userId);
  }
}
