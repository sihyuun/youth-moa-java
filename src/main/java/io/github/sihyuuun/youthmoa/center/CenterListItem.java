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
 * <p>F0h-center-desc-image (spec §9-1): description/imageUrl 의 진리 소스는 {@link CenterContent} 로 분리됨.
 * View 는 template 무변경을 위해 record 필드는 그대로 유지. 팩토리 {@link #of} 가 CenterContent 를 인자로 받아 매핑.
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
   * F0h-center-desc-image (spec §9-1): {@link CenterContent} 를 명시적 파라미터로 받아 desc/imageUrl 을 채운다.
   * content 가 null 이면 두 필드 null 로 두고 View 에서 fallback (설명 미노출 · placeholder 이미지).
   */
  public static CenterListItem of(
      Center c, int programCount, LocalDateTime now, boolean isHoliday, CenterContent content) {
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
        content != null ? content.getDescription() : null,
        c.getOperatingHours(),
        content != null ? content.getImageUrl() : null,
        openNow,
        c.hasSchedule());
  }

  /** 하위 호환 — content 미상 시 desc/imageUrl 없이 채움. 테스트·구 호출자용. */
  public static CenterListItem of(Center c, int programCount, LocalDateTime now, boolean isHoliday) {
    return of(c, programCount, now, isHoliday, null);
  }

  /** 하위 호환 — 카운트/시각 미상 시 현재 시각·공휴일 false 기본값 사용. 테스트·구 호출자용. */
  public static CenterListItem from(Center c) {
    return of(c, 0, LocalDateTime.now(), false, null);
  }

  public boolean hasCoordinates() {
    return latitude != null && longitude != null;
  }
}
