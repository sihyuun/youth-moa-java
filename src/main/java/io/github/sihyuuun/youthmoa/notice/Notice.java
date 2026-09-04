package io.github.sihyuuun.youthmoa.notice;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import io.github.sihyuuun.youthmoa.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "notice")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 255)
  private String title;

  /**
   * 260826 chore: @Lob → @JdbcTypeCode(LONGVARCHAR). Program 과 동일 이유. content 원문 검색 매칭 재개.
   * LONG32VARCHAR 는 실측 시 lower/upper 실패 → LONGVARCHAR 사용.
   */
  @JdbcTypeCode(SqlTypes.LONGVARCHAR)
  @Column(nullable = false)
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private NoticeCategory category;

  @Column(nullable = false)
  private boolean isPinned;

  @Column(nullable = false)
  private int viewCount;

  @Column(length = 500)
  private String imageUrl;

  /**
   * A-admin-notice-attachment (2026-09-03 · Qn-8 Custom): 작성자 기반 RBAC. SYSTEM_ADMIN 전 공지 편집 가능,
   * CENTER_ADMIN 본인 작성 공지만. V9 마이그레이션에서 기존 시드는 sysadmin 소유로 백필.
   */
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Builder
  private Notice(
      String title,
      String content,
      NoticeCategory category,
      Boolean isPinned,
      String imageUrl,
      User createdBy) {
    this.title = title;
    this.content = content;
    this.category = category != null ? category : NoticeCategory.NOTICE;
    this.isPinned = isPinned != null ? isPinned : false;
    this.viewCount = 0;
    this.imageUrl = imageUrl;
    this.createdBy = createdBy;
  }

  public void update(
      String title, String content, NoticeCategory category, boolean isPinned, String imageUrl) {
    this.title = title;
    this.content = content;
    this.category = category;
    this.isPinned = isPinned;
    this.imageUrl = imageUrl;
  }

  public void increaseViewCount() {
    this.viewCount++;
  }

  /** 홈 카드 등 legacy 템플릿의 ${n.tag} 참조 호환용. */
  public String getTag() {
    return category != null ? category.getLabel() : "";
  }
}
