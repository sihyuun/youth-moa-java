package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.program.Program;
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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
    name = "application",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uk_application_user_program",
            columnNames = {"user_id", "program_id"}))
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Application {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ApplicationStatus status;

  @Lob @Column private String applyReason;

  @Column(length = 500)
  private String rejectReason;

  /** D5: 신청 취소 사유 (CancelReason label + 사용자 입력 텍스트). nullable. */
  @Column(length = 200)
  private String cancelReason;

  /**
   * A4 admin-program-detail (V15 · Qn-1 A): 관리자 담당자 의견. prototype L2739 textarea. 사용자에게 노출될 수 있음
   * (placeholder 문구 기준). 상태 전이와 무관하게 저장 가능.
   */
  @Column(length = 1000)
  private String adminNote;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_by")
  private User processedBy;

  @Column private LocalDateTime processedAt;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime appliedAt;

  @Builder
  private Application(User user, Program program, ApplicationStatus status, String applyReason) {
    this.user = user;
    this.program = program;
    this.status = status != null ? status : ApplicationStatus.PENDING;
    this.applyReason = applyReason;
  }

  public void approve(User admin) {
    this.status = ApplicationStatus.APPROVED;
    this.processedBy = admin;
    this.processedAt = LocalDateTime.now();
    this.rejectReason = null;
  }

  public void reject(User admin, String reason) {
    this.status = ApplicationStatus.REJECTED;
    this.processedBy = admin;
    this.processedAt = LocalDateTime.now();
    this.rejectReason = reason;
  }

  public void cancel() {
    this.status = ApplicationStatus.CANCELLED;
  }

  /** D5: 사유와 함께 취소. reason 은 null 허용 (기존 호출부 호환). */
  public void cancel(String reason) {
    this.status = ApplicationStatus.CANCELLED;
    this.cancelReason = reason;
  }

  /**
   * A4 admin-program-detail (Qn-1 A): 관리자 담당자 의견 갱신. 상태 전이와 무관 · 재로드 시 그대로 유지. null/blank 는 컬럼
   * clear 처리.
   */
  public void updateAdminNote(String note) {
    this.adminNote = (note == null || note.isBlank()) ? null : note;
  }

  /**
   * A4 admin-program-detail (Qn-C A): 관리자 강제 취소. 사용자 취소(cancel) 와 도메인 메서드는 공유하되
   * processedBy·processedAt 을 기록해 감사 가능. reason 은 필수 (Bean Validation 은 Controller/Service 단에서 강제).
   */
  public void forceCancelByAdmin(User admin, String reason) {
    this.status = ApplicationStatus.CANCELLED;
    this.cancelReason = reason;
    this.processedBy = admin;
    this.processedAt = LocalDateTime.now();
  }

  /** CANCELLED 상태의 신청을 같은 row 로 재활성화 (DB unique constraint 우회). */
  public void reapply(String applyReason) {
    if (this.status != ApplicationStatus.CANCELLED) {
      throw new IllegalStateException("취소된 신청만 다시 활성화할 수 있습니다.");
    }
    this.status = ApplicationStatus.PENDING;
    this.applyReason = applyReason;
    this.processedBy = null;
    this.processedAt = null;
    this.rejectReason = null;
  }
}
