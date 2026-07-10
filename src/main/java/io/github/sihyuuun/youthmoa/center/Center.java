package io.github.sihyuuun.youthmoa.center;

import io.github.sihyuuun.youthmoa.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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

  /**
   * F0h-operating-hours-badge (spec §9-3): 영업 중단·폐업 kill-switch. 관리자가 폐업 처리한 센터는 {@code false} 로
   * 두어 배지가 시각 무관 "운영종료" 로 표시된다. 실시간 운영 여부는 {@link #isCurrentlyOpen}. 두 값이 모두 true 여야 "운영중"
   * 배지가 붙는다.
   */
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

  /**
   * F0h-operating-hours-badge (spec §9-1): 구조화된 요일별 운영시간. 배지 판정의 진리 소스. null 이면 판정 skip →
   * {@link #isCurrentlyOpen} 이 항상 false 반환하며, View 에서는 배지 자체 미노출 (spec §9-2 안전 default).
   */
  @Embedded
  private OperatingHours schedule;

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
      String imageUrl,
      OperatingHours schedule) {
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
    this.schedule = schedule;
  }

  /**
   * F0h-operating-hours-badge (spec §9-1): 주어진 시각의 실시간 운영 여부. schedule 미확보 시 false (안전 default,
   * spec §9-2). isActive kill-switch 는 호출자가 별도 조합 — {@code isActive && isCurrentlyOpen(now, isHoliday)}.
   */
  public boolean isCurrentlyOpen(LocalDateTime now, boolean isHoliday) {
    if (schedule == null) return false;
    return schedule.isOpenAt(now, isHoliday);
  }

  public boolean hasSchedule() {
    return schedule != null;
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
