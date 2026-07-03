package io.github.sihyuuun.youthmoa.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 홈·사이트 공용 이미지 슬롯. 관리자 페이지 도입 이후 CRUD 대상.
 *
 * <p>slot 은 고유 문자열 키(예: HERO_BANNER, HOME_SPACE_1) 로, 조회 시 slot 기반으로 접근한다.
 */
@Getter
@Entity
@Table(name = "site_image")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 100)
  private String slot;

  @Column(nullable = false, length = 500)
  private String imageUrl;

  @Column(nullable = false)
  private int sortOrder;

  @Column(nullable = false)
  private boolean isActive;

  /** 향후 확장 여지 — 공간 라벨 등 캡션. 현재는 미사용. */
  @Column(length = 100)
  private String caption;

  @Builder
  private SiteImage(
      String slot, String imageUrl, Integer sortOrder, Boolean isActive, String caption) {
    this.slot = slot;
    this.imageUrl = imageUrl;
    this.sortOrder = sortOrder != null ? sortOrder : 0;
    this.isActive = isActive != null ? isActive : true;
    this.caption = caption;
  }

  public void update(String imageUrl, int sortOrder, boolean isActive, String caption) {
    this.imageUrl = imageUrl;
    this.sortOrder = sortOrder;
    this.isActive = isActive;
    this.caption = caption;
  }
}
