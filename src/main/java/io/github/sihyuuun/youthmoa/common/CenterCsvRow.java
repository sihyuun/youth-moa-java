package io.github.sihyuuun.youthmoa.common;

import io.github.sihyuuun.youthmoa.center.OperatingHours;
import java.math.BigDecimal;

/**
 * F0h-real-coords: {@code src/main/resources/data/centers.csv} 한 행 표현.
 *
 * <p>파생 시드(과거 regionCoords + offset) 제거 후, Center 실좌표·전화·운영시간을 CSV row 자체가 진리 소스로 제공한다.
 *
 * <p>F0h-operating-hours-badge (spec §9-4): 15컬럼 확장 — 신규 필드 {@code schedule} 은 CSV 9~15컬럼
 * (weekdayOpen/Close, saturdayOpen/Close, sundayOpen/Close, holidayClosed) 을 파싱한 값 객체. 파싱 불가 행
 * (weekday 컬럼 모두 빈값) 은 {@code null} — spec §9-2 안전 default.
 */
public record CenterCsvRow(
    String name,
    String region,
    String address,
    BigDecimal latitude,
    BigDecimal longitude,
    String phone,
    String operatingHours,
    boolean isActive,
    OperatingHours schedule) {}
