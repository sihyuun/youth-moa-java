package io.github.sihyuuun.youthmoa.notification;

import io.github.sihyuuun.youthmoa.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(
    name = "notification",
    indexes = {
      @Index(name = "idx_notification_user_read", columnList = "user_id, isRead"),
      @Index(name = "idx_notification_user_created", columnList = "user_id, createdAt DESC"),
      // A7-rate-limit (2026-09-29): findMergeCandidate 조회 커버 인덱스.
      @Index(
          name = "idx_notification_dedup",
          columnList = "user_id, type, dedup_key, last_occurred_at DESC")
    })
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private NotificationType type;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, length = 500)
  private String message;

  @Column(length = 500)
  private String link;

  @Column(nullable = false)
  private boolean isRead;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  // ── A7-rate-limit (2026-09-29) 필드 ─────────────────────────────────
  /**
   * 병합 대상 그룹 키. 형식 예: "NEW_APPLICATION:{programId}" / "NEW_USER:global". null 이면 병합 대상 아님 (사용자 트랙
   * NotificationService.create 로 발행된 알림).
   */
  @Column(name = "dedup_key", length = 120)
  private String dedupKey;

  /** 병합된 원시 이벤트 수. UI 는 > 1 일 때 "[n건] title" 부기. */
  @Column(name = "occurrence_count", nullable = false)
  private int occurrenceCount;

  /**
   * 마지막 발생 시각. 병합 판정 window (`now - mergeWindow`) 및 헤더 드롭다운 최근순 정렬 기준. 최초 발행 시 createdAt 과 동일 시각으로
   * 세팅되고, 병합 시 최신 시각으로 갱신.
   */
  @Column(name = "last_occurred_at", nullable = false)
  private LocalDateTime lastOccurredAt;

  @Builder
  private Notification(
      User user,
      NotificationType type,
      String title,
      String message,
      String link,
      String dedupKey) {
    this.user = user;
    this.type = type;
    this.title = title;
    this.message = message;
    this.link = link;
    this.dedupKey = dedupKey;
    this.isRead = false;
    this.occurrenceCount = 1;
    // lastOccurredAt 은 @PrePersist 훅에서 createdAt 과 동일 시각으로 세팅. Builder 시점엔 createdAt 이 null.
  }

  /**
   * A7-rate-limit (2026-09-29): 최초 persist 시 lastOccurredAt 을 createdAt 과 동기화. Auditing 리스너가
   * createdAt 을 세팅한 뒤 이 훅이 실행되도록 EntityListeners 순서에 의존 — Spring Data JPA 기본 순서상
   * AuditingEntityListener 는 @PrePersist 로 등록되므로 이 클래스의 @PrePersist 훅과 함께 실행된다. 안전하게 createdAt 이
   * 여전히 null 이면 now() 로 폴백.
   */
  @PrePersist
  private void onPrePersist() {
    if (this.lastOccurredAt == null) {
      this.lastOccurredAt = this.createdAt != null ? this.createdAt : LocalDateTime.now();
    }
  }

  public void markAsRead() {
    this.isRead = true;
  }

  /**
   * A7-rate-limit (2026-09-29): 동일 dedupKey 알림 병합 시 호출.
   *
   * <ul>
   *   <li>occurrenceCount++
   *   <li>lastOccurredAt = now
   *   <li>isRead = false 로 리셋 (사용자 결정: 재unread → badge 재알림)
   * </ul>
   *
   * title/message/link 는 별도 updateContent 로 최신 값으로 덮어쓴다.
   */
  public void mergeOccurrence(LocalDateTime now) {
    this.occurrenceCount++;
    this.lastOccurredAt = now;
    this.isRead = false;
  }

  /** 병합 시 최신 컨텍스트로 텍스트 갱신. 최신 신청자 이름 등 message 가 바뀌어야 자연스러운 UX. link 는 대개 동일하지만 안전하게 재세팅. */
  public void updateContent(String title, String message, String link) {
    this.title = title;
    this.message = message;
    this.link = link;
  }
}
