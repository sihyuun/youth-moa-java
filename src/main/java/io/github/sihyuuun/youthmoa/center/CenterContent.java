package io.github.sihyuuun.youthmoa.center;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * F0h-center-desc-image (spec §9-1): 마케팅 콘텐츠(설명·이미지) 를 Center 팩트에서 분리한 owning-side 엔티티.
 *
 * <p>CLAUDE.md §확장성 원칙 §파생 시드 금지 정합: description·imageUrl 은 각 row 자체가 진리 소스. 관리자 CRUD 대상.
 *
 * <p>관계는 {@code CenterContent} 소유(FK center_id UNIQUE). Center 는 역방향 참조하지 않는다 (양방향 금지 원칙 준수).
 */
@Getter
@Entity
@Table(name = "center_content")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CenterContent extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "center_id", nullable = false, unique = true)
  private Center center;

  @Column(length = 500)
  private String description;

  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Builder
  private CenterContent(Center center, String description, String imageUrl) {
    this.center = center;
    this.description = description;
    this.imageUrl = imageUrl;
  }

  public void updateDescription(String description) {
    this.description = description;
  }

  public void updateImageUrl(String imageUrl) {
    this.imageUrl = imageUrl;
  }

  public void update(String description, String imageUrl) {
    this.description = description;
    this.imageUrl = imageUrl;
  }
}
