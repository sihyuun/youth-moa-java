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

  @Builder
  private Center(
      String name,
      String region,
      String address,
      String phone,
      Boolean isActive,
      Boolean isFeatured,
      BigDecimal latitude,
      BigDecimal longitude) {
    this.name = name;
    this.region = region;
    this.address = address;
    this.phone = phone;
    this.isActive = isActive != null ? isActive : true;
    this.isFeatured = isFeatured != null && isFeatured;
    this.latitude = latitude;
    this.longitude = longitude;
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
