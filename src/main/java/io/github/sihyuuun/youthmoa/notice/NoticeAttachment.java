package io.github.sihyuuun.youthmoa.notice;

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
 * 공지사항 첨부파일 (F0g).
 *
 * <p>학습 단계에서는 실제 파일 저장 인프라 없이 메타데이터만 관리하고 다운로드는 JS alert stub 처리. storedName 은 향후 실 파일 경로로 승격 예정.
 */
@Getter
@Entity
@Table(name = "notice_attachment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class NoticeAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "notice_id", nullable = false)
  private Notice notice;

  @Column(nullable = false, length = 255)
  private String fileName;

  @Column(length = 255)
  private String storedName;

  @Column(nullable = false)
  private long fileSize;

  @Column(length = 100)
  private String contentType;

  @Column(nullable = false)
  private int sortOrder;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Builder
  private NoticeAttachment(
      Notice notice,
      String fileName,
      String storedName,
      long fileSize,
      String contentType,
      Integer sortOrder) {
    this.notice = notice;
    this.fileName = fileName;
    this.storedName = storedName;
    this.fileSize = fileSize;
    this.contentType = contentType;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
  }

  /** 사람이 읽는 파일 크기 표기 (예: "1.2MB"). */
  public String getHumanFileSize() {
    if (fileSize < 1024) return fileSize + "B";
    if (fileSize < 1024 * 1024) return String.format("%.1fKB", fileSize / 1024.0);
    return String.format("%.1fMB", fileSize / 1024.0 / 1024.0);
  }
}
