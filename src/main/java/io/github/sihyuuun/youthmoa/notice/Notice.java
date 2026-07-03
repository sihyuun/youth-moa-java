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

  @Builder
  private Notice(
      String title, String content, NoticeCategory category, Boolean isPinned, String imageUrl) {
    this.title = title;
    this.content = content;
    this.category = category != null ? category : NoticeCategory.NOTICE;
    this.isPinned = isPinned != null ? isPinned : false;
    this.viewCount = 0;
    this.imageUrl = imageUrl;
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
