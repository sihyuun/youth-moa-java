package io.github.sihyuuun.youthmoa.center;

import java.math.BigDecimal;

/**
 * 청년센터 목록 카드 + 지도 마커 + 인포윈도우에 필요한 최소 필드.
 *
 * <p>F0h-c2: programCount 추가 (진행중 프로그램 수). F0h-c1/c4: description/operatingHours/imageUrl 추가.
 * 좌표는 nullable — 미확정 센터는 리스트만 표시하고 마커 skip.
 */
public record CenterListItem(
    Long id,
    String name,
    String region,
    String address,
    String phone,
    BigDecimal latitude,
    BigDecimal longitude,
    boolean isActive,
    int programCount,
    String description,
    String operatingHours,
    String imageUrl) {

  public static CenterListItem of(Center c, int programCount) {
    return new CenterListItem(
        c.getId(),
        c.getName(),
        c.getRegion(),
        c.getAddress(),
        c.getPhone(),
        c.getLatitude(),
        c.getLongitude(),
        c.isActive(),
        programCount,
        c.getDescription(),
        c.getOperatingHours(),
        c.getImageUrl());
  }

  /** 하위 호환용 — 카운트 미상 시 0 으로 세팅. */
  public static CenterListItem from(Center c) {
    return of(c, 0);
  }

  public boolean hasCoordinates() {
    return latitude != null && longitude != null;
  }
}
