package io.github.sihyuuun.youthmoa.notification;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

  /**
   * F0f-fix-5: 알림 목록을 오늘/지난 7일/이전 3개 그룹으로 반환.
   *
   * @param unreadOnly true 면 읽지 않은 알림만
   * @return LinkedHashMap ("today"/"week"/"earlier" 순서 보장). 각 그룹 최근순 정렬. 비어있는 그룹은 미포함.
   */
  public Map<String, List<Notification>> findGrouped(User user, boolean unreadOnly) {
    // F0f-fix-5 verify: listAll(20건 상한) 대신 전체 조회로 그룹핑 (필터 pill 카운트와 정합).
    List<Notification> source = notificationRepository.findAllByUserOrderByCreatedAtDesc(user);
    LocalDate today = LocalDate.now();
    LocalDateTime todayStart = today.atStartOfDay();
    LocalDateTime weekStart = today.minusDays(6).atStartOfDay();

    Map<String, List<Notification>> grouped = new LinkedHashMap<>();
    grouped.put("today", new java.util.ArrayList<>());
    grouped.put("week", new java.util.ArrayList<>());
    grouped.put("earlier", new java.util.ArrayList<>());

    for (Notification n : source) {
      if (unreadOnly && n.isRead()) continue;
      LocalDateTime c = n.getCreatedAt();
      if (!c.isBefore(todayStart)) grouped.get("today").add(n);
      else if (!c.isBefore(weekStart)) grouped.get("week").add(n);
      else grouped.get("earlier").add(n);
    }
    // 빈 그룹 제거
    grouped.entrySet().removeIf(e -> e.getValue().isEmpty());
    return grouped;
  }

  /** 필터 pill 전체 카운트 (그룹핑 소스와 동일 스코프). */
  public long totalCount(User user) {
    return notificationRepository.findAllByUserOrderByCreatedAtDesc(user).size();
  }

  /**
   * 알림 신규 생성.
   *
   * <p>userId 로 User 프록시(getReferenceById)를 사용해 SELECT 1회 절약. 유저가 실제로 존재하지 않으면 flush 시점에 FK 위반이 나므로
   * 호출자는 유효한 userId 를 넘겨야 한다.
   */
  @Transactional
  public Notification create(
      Long userId, NotificationType type, String title, String message, String link) {
    User user = userRepository.getReferenceById(userId);
    Notification n =
        Notification.builder()
            .user(user)
            .type(type)
            .title(title)
            .message(message)
            .link(link)
            .build();
    return notificationRepository.save(n);
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

  /**
   * 개별 알림 삭제 (hard delete). 소유자 검증 후 즉시 제거.
   *
   * <p>prototype L1305 close(X) 정합. Notification 엔티티에 soft-delete 컬럼이 없으므로 hard delete.
   *
   * @throws org.springframework.web.server.ResponseStatusException 404 — 존재하지 않거나 다른 유저 알림
   */
  @Transactional
  public void delete(Long notificationId, Long userId) {
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
    notificationRepository.delete(n);
  }
}
