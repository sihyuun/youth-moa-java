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
