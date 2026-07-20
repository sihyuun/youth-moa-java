package io.github.sihyuuun.youthmoa.notification;

import io.github.sihyuuun.youthmoa.user.User;
import java.util.List;
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

  /** 헤더 드롭다운용 — 최근 5건. */
  List<Notification> findTop5ByUserOrderByCreatedAtDesc(User user);

  long countByUserAndIsReadFalse(User user);

  @Modifying
  @Query("update Notification n set n.isRead = true where n.user = :user and n.isRead = false")
  int markAllAsRead(@Param("user") User user);
}
