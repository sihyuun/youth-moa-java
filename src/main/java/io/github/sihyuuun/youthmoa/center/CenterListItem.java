package io.github.sihyuuun.youthmoa.center;

import java.math.BigDecimal;

/** 청년센터 목록 카드 + 지도 마커에 필요한 최소 필드. 좌표(lat/lng) 는 nullable — 미확정 센터는 리스트만 표시하고 마커는 skip. */
public record CenterListItem(
    Long id,
    String name,
    String region,
    String address,
    String phone,
    BigDecimal latitude,
    BigDecimal longitude,
    boolean isActive) {

  public static CenterListItem from(Center c) {
    return new CenterListItem(
        c.getId(),
        c.getName(),
        c.getRegion(),
        c.getAddress(),
        c.getPhone(),
        c.getLatitude(),
        c.getLongitude(),
        c.isActive());
  }

  public boolean hasCoordinates() {
    return latitude != null && longitude != null;
  }
}
