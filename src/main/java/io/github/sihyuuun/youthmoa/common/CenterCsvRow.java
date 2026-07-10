package io.github.sihyuuun.youthmoa.common;

import java.math.BigDecimal;

/**
 * F0h-real-coords: {@code src/main/resources/data/centers.csv} 한 행 표현.
 *
 * <p>파생 시드(과거 regionCoords + offset) 제거 후, Center 실좌표·전화·운영시간을 CSV row 자체가 진리 소스로
 * 제공한다. description/imageUrl 은 이번 스코프 밖 (후속 티켓 fix/F0h-center-desc-image).
 */
public record CenterCsvRow(
    String name,
    String region,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String phone,
    String operatingHours,
    boolean isActive) {}
