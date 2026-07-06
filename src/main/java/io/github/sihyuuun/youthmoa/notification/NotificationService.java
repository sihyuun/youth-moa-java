package io.github.sihyuuun.youthmoa.notification;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 조회·읽음 처리 서비스.
 *
 * <p>{@code @Service} — Spring bean 등록. {@code @Transactional(readOnly=true)} 클래스 레벨 부착 → 조회 메서드는
 * read-only 트랜잭션에서 동작. 쓰기 메서드는 개별 {@code @Transactional} 로 오버라이드.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final UserRepository userRepository;

  /** 헤더 종용 — 최근 5건. */
  public List<Notification> recentForHeader(User user) {
    return notificationRepository.findTop5ByUserOrderByCreatedAtDesc(user);
  }

  public long unreadCount(User user) {
    return notificationRepository.countByUserAndIsReadFalse(user);
  }

  /** 전체 목록 (F2d 페이지네이션 예정, 지금은 최근 20건). */
  public List<Notification> listAll(User user) {
    return notificationRepository
        .findAllByUserOrderByCreatedAtDesc(
            user, org.springframework.data.domain.PageRequest.of(0, 20))
        .getContent();
  }

  @Transactional
  public int markAllAsRead(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
    return notificationRepository.markAllAsRead(user);
  }

  /** 개별 읽음 처리. 다른 유저 알림 → 404 (권한 노출 방지). */
  @Transactional
  public Notification markAsRead(Long notificationId, Long userId) {
    Notification n =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(
                () ->
                    new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND));
    if (!n.getUser().getId().equals(userId)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.NOT_FOUND);
    }
    n.markAsRead();
    return n;
  }
}
