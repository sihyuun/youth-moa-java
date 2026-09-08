package io.github.sihyuuun.youthmoa.application;

import io.github.sihyuuun.youthmoa.program.ApplyQuestion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * F0c-dynamic-fields (2026-09-08 · Qn-6 A): 사용자 신청에 대한 동적 질문 응답 row.
 *
 * <p>row per (application × question). TEXT/DROPDOWN 은 {@link #value} 사용, ATTACHMENT 는 {@link
 * #attachmentPath} + filename + size 사용.
 *
 * <p>응답 수정 UX 는 이번 스코프 밖 → updatedAt 없음.
 */
@Getter
@Entity
@Table(name = "apply_answer")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApplyAnswer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "application_id", nullable = false)
  private Application application;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "question_id", nullable = false)
  private ApplyQuestion question;

  @Column(columnDefinition = "TEXT")
  private String value;

  @Column(name = "attachment_path", length = 500)
  private String attachmentPath;

  @Column(name = "attachment_filename", length = 200)
  private String attachmentFilename;

  @Column(name = "attachment_size")
  private Long attachmentSize;

  @CreatedDate
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Builder
  private ApplyAnswer(
      Application application,
      ApplyQuestion question,
      String value,
      String attachmentPath,
      String attachmentFilename,
      Long attachmentSize) {
    this.application = application;
    this.question = question;
    this.value = value;
    this.attachmentPath = attachmentPath;
    this.attachmentFilename = attachmentFilename;
    this.attachmentSize = attachmentSize;
  }
}
