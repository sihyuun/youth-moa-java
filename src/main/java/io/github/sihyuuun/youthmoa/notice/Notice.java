package io.github.sihyuuun.youthmoa.notice;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

  @Lob
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
   * 260826 P9 후속: 통합 검색 대상용 요약 텍스트. content 는 @Lob → CLOB 이라 upper(CLOB) 불가. VARCHAR(300) summary 를
   * 별도 관리해 검색 매칭에만 사용 (화면 노출 X). NULL 허용.
   */
  @Column(length = 300)
  private String summary;

  @Builder
  private Notice(
      String title,
      String content,
      String summary,
      NoticeCategory category,
      Boolean isPinned,
      String imageUrl) {
    this.title = title;
    this.content = content;
    this.summary = summary;
    this.category = category != null ? category : NoticeCategory.NOTICE;
    this.isPinned = isPinned != null ? isPinned : false;
    this.viewCount = 0;
    this.imageUrl = imageUrl;
  }

  public void update(
      String title,
      String content,
      String summary,
      NoticeCategory category,
      boolean isPinned,
      String imageUrl) {
    this.title = title;
    this.content = content;
    this.summary = summary;
    this.category = category;
    this.isPinned = isPinned;
    this.imageUrl = imageUrl;
  }

  public void increaseViewCount() {
    this.viewCount++;
  }

  /** 260826 P9 후속: summary 가 명시되지 않았으면 content 앞 300자로 자동 파생. DataInitializer 시드 편의 목적. */
  public void deriveSummaryFromContentIfMissing() {
    if (this.summary == null && this.content != null) {
      this.summary = this.content.length() > 300 ? this.content.substring(0, 300) : this.content;
    }
  }

  /** 홈 카드 등 legacy 템플릿의 ${n.tag} 참조 호환용. */
  public String getTag() {
    return category != null ? category.getLabel() : "";
  }
}
