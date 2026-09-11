package io.github.sihyuuun.youthmoa.program;

import jakarta.persistence.Basic;
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
 * A3-2 admin-program-form-integration (2026-09-11 · Qn-C A · NoticeAttachment 승계): 프로그램 첨부파일.
 *
 * <p>학습 단계 인프라 유예 정책 승계 — {@code @Lob byte[] data} 컬럼 유지 (LocalFileStorage 폴백 + 다운로드 endpoint 호환).
 * 상한 5MB / 확장자 pdf·hwp·docx·xlsx / 개수 10개 정책은 {@code AdminProgramAttachmentService} 상수로 관리.
 */
@Getter
@Entity
@Table(name = "program_attachment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ProgramAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "program_id", nullable = false)
  private Program program;

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

  /**
   * 실 파일 바이트 (NoticeAttachment 승계). LAZY fetch 로 목록 조회 시 로드하지 않음. Supabase Storage 이관 시 nullable +
   * storageUrl 로 승격 예정.
   */
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "data", columnDefinition = "bytea")
  private byte[] data;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Builder
  private ProgramAttachment(
      Program program,
      String fileName,
      String storedName,
      long fileSize,
      String contentType,
      Integer sortOrder,
      byte[] data) {
    this.program = program;
    this.fileName = fileName;
    this.storedName = storedName;
    this.fileSize = fileSize;
    this.contentType = contentType;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
    this.data = data;
  }

  /** 사람이 읽는 파일 크기 표기 (예: "1.2MB"). */
  public String getHumanFileSize() {
    if (fileSize < 1024) return fileSize + "B";
    if (fileSize < 1024 * 1024) return String.format("%.1fKB", fileSize / 1024.0);
    return String.format("%.1fMB", fileSize / 1024.0 / 1024.0);
  }
}
