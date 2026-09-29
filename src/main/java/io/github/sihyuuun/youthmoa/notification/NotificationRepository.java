package io.github.sihyuuun.youthmoa.notification;

import io.github.sihyuuun.youthmoa.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findAllByUserOrderByCreatedAtDesc(User user, Pageable pageable);

  /** F0f-fix-5: 그룹핑용 — 페이지 상한 없이 사용자 알림 전체 (최근 순). 향후 페이지네이션 도입 시 조정. */
  List<Notification> findAllByUserOrderByCreatedAtDesc(User user);

  /**
   * 헤더 드롭다운용 — 최근 5건.
   *
   * <p>A7-rate-limit (2026-09-29): 정렬 기준을 createdAt → lastOccurredAt 으로 변경. 병합된 알림이 최신 발생 시점 기준으로
   * 상단에 노출되도록. 최초 발행 시 lastOccurredAt 은 createdAt 과 동일하므로 사용자 트랙(dedupKey=null · 병합 없음)에도 무영향.
   */
  List<Notification> findTop5ByUserOrderByLastOccurredAtDesc(User user);

  long countByUserAndIsReadFalse(User user);

  @Modifying
  @Query("update Notification n set n.isRead = true where n.user = :user and n.isRead = false")
  int markAllAsRead(@Param("user") User user);

  /**
   * A7-rate-limit E2E fix (2026-09-29): 특정 사용자의 알림 row 를 전량 삭제한다.
   *
   * <p>e2e {@code TestFixtureController.resetNotifications} 전용. mark-all-read 만 하는 헬퍼로는
   * admin-notification-rate-limit spec 의 TC(b) window 밖 재신청 시나리오를 격리할 수 없다 (TC(a) 잔존 병합 row 가 Top5
   * 결과에 남아 예상 2 rows → 실측 3 rows 로 오검출). Spring Data 파생 쿼리 → bulk DELETE.
   */
  @Modifying
  int deleteAllByUserId(Long userId);

  /**
   * A7-rate-limit (2026-09-29): 동일 (user, type, dedupKey) 그룹의 window 내 최신 후보 조회.
   *
   * <p>Q3 그룹기준 = type + sourceId (dedupKey 에 인코딩됨). since = now - mergeWindow. 여러 row 가 걸리면
   * lastOccurredAt DESC 로 가장 최신 1건 반환.
   *
   * <p>Q5 사용자 트랙 무영향 — dedupKey IS NULL 인 사용자 트랙 알림은 이 쿼리에 매칭되지 않는다 (dedupKey = null 은 등호 비교 실패).
   * 인덱스 커버: idx_notification_dedup(user_id, type, dedup_key, last_occurred_at DESC).
   */
  @Query(
      "SELECT n FROM Notification n"
          + " WHERE n.user.id = :userId AND n.type = :type"
          + " AND n.dedupKey = :dedupKey AND n.lastOccurredAt >= :since"
          + " ORDER BY n.lastOccurredAt DESC")
  List<Notification> findMergeCandidates(
      @Param("userId") Long userId,
      @Param("type") NotificationType type,
      @Param("dedupKey") String dedupKey,
      @Param("since") LocalDateTime since,
      Pageable pageable);

  default Optional<Notification> findMergeCandidate(
      Long userId, NotificationType type, String dedupKey, LocalDateTime since) {
    List<Notification> list =
        findMergeCandidates(
            userId, type, dedupKey, since, org.springframework.data.domain.PageRequest.of(0, 1));
    return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
  }
}
