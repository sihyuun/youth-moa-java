package io.github.sihyuuun.youthmoa.user;

import java.time.LocalDate;
import java.time.Period;

/**
 * A6 (2026-09-16 신설) — 사용자 연령대 그룹.
 *
 * <p>User.birthDate 는 저장만 있고 나이·연령대 계산 로직은 지금까지 없었다. A6 통계 대시보드가 최초 사용자.
 *
 * <p>구간(prototype L790~900 + 40+ 확장):
 *
 * <ul>
 *   <li>{@link #AGE_19_24} 19-24 세
 *   <li>{@link #AGE_25_29} 25-29 세
 *   <li>{@link #AGE_30_34} 30-34 세
 *   <li>{@link #AGE_35_39} 35-39 세
 *   <li>{@link #AGE_40_PLUS} 40 세 이상
 *   <li>{@link #UNKNOWN} birthDate == null (연령 정보 없음)
 * </ul>
 */
public enum AgeBucket {
  AGE_19_24("19-24세"),
  AGE_25_29("25-29세"),
  AGE_30_34("30-34세"),
  AGE_35_39("35-39세"),
  AGE_40_PLUS("40세 이상"),
  UNKNOWN("미상");

  private final String label;

  AgeBucket(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  /** birthDate 기준으로 today 시점의 만 나이를 계산하여 구간을 반환한다. birthDate == null 이면 UNKNOWN. */
  public static AgeBucket of(LocalDate birthDate, LocalDate today) {
    if (birthDate == null || today == null) return UNKNOWN;
    if (birthDate.isAfter(today)) return UNKNOWN;
    int age = Period.between(birthDate, today).getYears();
    if (age < 19) return UNKNOWN; // 학습 프로젝트: 청년 19+ 대상. 그 외는 UNKNOWN 취급.
    if (age <= 24) return AGE_19_24;
    if (age <= 29) return AGE_25_29;
    if (age <= 34) return AGE_30_34;
    if (age <= 39) return AGE_35_39;
    return AGE_40_PLUS;
  }

  /** {@link #of(LocalDate, LocalDate)} 의 편의 오버로드 — today = {@link LocalDate#now()}. */
  public static AgeBucket of(LocalDate birthDate) {
    return of(birthDate, LocalDate.now());
  }
}
