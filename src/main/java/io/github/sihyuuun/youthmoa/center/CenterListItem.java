package io.github.sihyuuun.youthmoa.center;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 청년센터 목록 카드 + 지도 마커 + 인포윈도우에 필요한 최소 필드.
 *
 * <p>F0h-c2: programCount 추가 (진행중 프로그램 수). F0h-c1/c4: description/operatingHours/imageUrl 추가.
 * F0h-operating-hours-badge (spec §9-1, §9-5): {@code isOpenNow} + {@code hasSchedule} 추가 —
 * Controller 에서 사전 계산해 View 렌더 부담 감소. schedule 없는 센터는 배지 자체 미노출.
 *
 * <p>좌표는 nullable — 미확정 센터는 리스트만 표시하고 마커 skip.
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
    String imageUrl,
    boolean isOpenNow,
    boolean hasSchedule) {

  /**
   * F0h-operating-hours-badge: 배지 판정용 시각·공휴일 파라미터를 받아 실시간 운영 여부 계산 후 필드에 채운다.
   */
  public static CenterListItem of(Center c, int programCount, LocalDateTime now, boolean isHoliday) {
    boolean openNow = c.isCurrentlyOpen(now, isHoliday);
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
        c.getImageUrl(),
        openNow,
        c.hasSchedule());
  }

  /**
   * 하위 호환 — 카운트/시각 미상 시 현재 시각·공휴일 false 기본값 사용. 테스트·구 호출자용.
   */
  public static CenterListItem from(Center c) {
    return of(c, 0, LocalDateTime.now(), false);
  }

  public boolean hasCoordinates() {
    return latitude != null && longitude != null;
  }
}
