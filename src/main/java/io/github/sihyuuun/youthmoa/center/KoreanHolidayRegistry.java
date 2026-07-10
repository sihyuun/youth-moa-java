package io.github.sihyuuun.youthmoa.center;

import java.time.LocalDate;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * F0h-operating-hours-badge (spec §9-2): 한국 공휴일 판정.
 *
 * <p><b>임시 하드코딩 구현</b> (2026·2027). jollyday 라이브러리는 Maven Central 좌표 확인 후 별도 티켓에서
 * 전환 예정. 하드코딩 대상: 신정, 삼일절, 어린이날, 현충일, 광복절, 개천절, 한글날, 성탄절 (고정일) + 음력
 * 공휴일 (설날 연휴 3일, 추석 연휴 3일, 부처님오신날) 은 연도별 실 날짜 지정.
 *
 * <p>2028년 이후 사용 시 이 클래스 확장 필요 (혹은 jollyday 도입).
 */
@Component
public class KoreanHolidayRegistry {

  private static final Set<LocalDate> HOLIDAYS =
      Set.of(
          // ── 2026 공휴일 ──
          LocalDate.of(2026, 1, 1),   // 신정
          LocalDate.of(2026, 2, 16),  // 설날 (음력 1/1)
          LocalDate.of(2026, 2, 17),  // 설날 연휴
          LocalDate.of(2026, 2, 18),  // 설날 연휴
          LocalDate.of(2026, 3, 1),   // 삼일절
          LocalDate.of(2026, 3, 2),   // 삼일절 대체휴일 (일요일 겹침)
          LocalDate.of(2026, 5, 5),   // 어린이날
          LocalDate.of(2026, 5, 24),  // 부처님오신날 (음력 4/8)
          LocalDate.of(2026, 5, 25),  // 부처님오신날 대체휴일
          LocalDate.of(2026, 6, 6),   // 현충일
          LocalDate.of(2026, 8, 15),  // 광복절
          LocalDate.of(2026, 8, 17),  // 광복절 대체휴일 (토요일 겹침)
          LocalDate.of(2026, 9, 24),  // 추석 연휴
          LocalDate.of(2026, 9, 25),  // 추석 (음력 8/15)
          LocalDate.of(2026, 9, 26),  // 추석 연휴
          LocalDate.of(2026, 10, 3),  // 개천절
          LocalDate.of(2026, 10, 5),  // 개천절 대체휴일 (토요일 겹침)
          LocalDate.of(2026, 10, 9),  // 한글날
          LocalDate.of(2026, 12, 25), // 성탄절
          // ── 2027 공휴일 ──
          LocalDate.of(2027, 1, 1),   // 신정
          LocalDate.of(2027, 2, 6),   // 설날 연휴 (토)
          LocalDate.of(2027, 2, 7),   // 설날 (음력 1/1, 일)
          LocalDate.of(2027, 2, 8),   // 설날 연휴 (월)
          LocalDate.of(2027, 2, 9),   // 설날 대체휴일 (연휴 중 일요일 겹침, 화)
          LocalDate.of(2027, 3, 1),   // 삼일절
          LocalDate.of(2027, 5, 5),   // 어린이날
          LocalDate.of(2027, 5, 13),  // 부처님오신날 (음력 4/8)
          LocalDate.of(2027, 6, 6),   // 현충일 (일요일 → 대체휴일 6/7)
          LocalDate.of(2027, 6, 7),   // 현충일 대체휴일 (월)
          LocalDate.of(2027, 8, 15),  // 광복절 (일요일 → 대체휴일 8/16)
          LocalDate.of(2027, 8, 16),  // 광복절 대체휴일 (월)
          LocalDate.of(2027, 9, 14),  // 추석 연휴 (화)
          LocalDate.of(2027, 9, 15),  // 추석 (음력 8/15, 수)
          LocalDate.of(2027, 9, 16),  // 추석 연휴 (목)
          LocalDate.of(2027, 10, 3),  // 개천절 (일요일 → 대체휴일 10/4)
          LocalDate.of(2027, 10, 4),  // 개천절 대체휴일 (월)
          LocalDate.of(2027, 10, 9),  // 한글날 (토 → 대체휴일 10/11, 2021 개정)
          LocalDate.of(2027, 10, 11), // 한글날 대체휴일 (월)
          LocalDate.of(2027, 12, 25), // 성탄절 (토 → 대체휴일 12/27, 2023 개정)
          LocalDate.of(2027, 12, 27)  // 성탄절 대체휴일 (월)
          );

  public boolean isHoliday(LocalDate date) {
    if (date == null) return false;
    return HOLIDAYS.contains(date);
  }
}
