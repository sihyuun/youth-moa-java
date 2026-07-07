package io.github.sihyuuun.youthmoa.center;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "center")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Center extends BaseTimeEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @Column(nullable = false, length = 50)
  private String region;

  @Column(length = 255)
  private String address;

  @Column(length = 20)
  private String phone;

  @Column(nullable = false)
  private boolean isActive;

  @Column(nullable = false)
  private boolean isFeatured;

  /** 위도 (WGS84). BigDecimal(10,7) — 소수 7자리 = ±1cm 정밀. 좌표 미확정 센터는 null (마커 skip, 리스트만 표시). */
  @Column(precision = 10, scale = 7)
  private BigDecimal latitude;

  /** 경도 (WGS84). BigDecimal(10,7). */
  @Column(precision = 10, scale = 7)
  private BigDecimal longitude;

  /** F0h-c1: 센터 서브 텍스트 (prototype `desc`). null 허용. */
  @Column(length = 500)
  private String description;

  /** F0h-c1: 운영시간 자유서식 (prototype `hours`). null 허용, 기본 시드 "평일 09:00~18:00". */
  @Column(name = "operating_hours", length = 100)
  private String operatingHours;

  /** F0h-c1: 카드/상세 이미지 URL (상대·절대). null 이면 placeholder 표시. */
  @Column(name = "image_url", length = 500)
  private String imageUrl;

  @Builder
  private Center(
      String name,
      String region,
      String address,
      String phone,
      Boolean isActive,
      Boolean isFeatured,
      BigDecimal latitude,
      BigDecimal longitude,
      String description,
      String operatingHours,
      String imageUrl) {
    this.name = name;
    this.region = region;
    this.address = address;
    this.phone = phone;
    this.isActive = isActive != null ? isActive : true;
    this.isFeatured = isFeatured != null && isFeatured;
    this.latitude = latitude;
    this.longitude = longitude;
    this.description = description;
    this.operatingHours = operatingHours;
    this.imageUrl = imageUrl;
  }

  /** F0h-c1: 관리자 페이지 확장 대비 컨텐츠 갱신 도메인 메서드. */
  public void updateContent(String description, String operatingHours, String imageUrl) {
    this.description = description;
    this.operatingHours = operatingHours;
    this.imageUrl = imageUrl;
  }

  public void markFeatured() {
    this.isFeatured = true;
  }

  public void unmarkFeatured() {
    this.isFeatured = false;
  }

  public void updateInfo(String name, String region, String address, String phone) {
    this.name = name;
    this.region = region;
    this.address = address;
    this.phone = phone;
  }

  public void updateCoordinates(BigDecimal latitude, BigDecimal longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public void activate() {
    this.isActive = true;
  }

  public void deactivate() {
    this.isActive = false;
  }

  public boolean hasCoordinates() {
    return latitude != null && longitude != null;
  }
}
