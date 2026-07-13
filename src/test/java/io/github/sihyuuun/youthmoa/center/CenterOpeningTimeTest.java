package io.github.sihyuuun.youthmoa.center;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * F0h-operating-hours-badge (spec §6): OperatingHours + Center.isCurrentlyOpen 시나리오별 회귀 방어.
 *
 * <p>구조: 순수 POJO 단위 테스트 — Spring context / DB 필요 없음. 시각 파라미터 방식(spec §9-6) 덕분에 명시적 LocalDateTime
 * 주입으로 결정론적 검증 가능.
 */
class CenterOpeningTimeTest {

  // 기준 스케줄: 평일 10~21, 토 10~17, 일 미운영, 공휴일 휴관
  private OperatingHours defaultSchedule() {
    return OperatingHours.builder()
        .weekdayOpen(LocalTime.of(10, 0))
        .weekdayClose(LocalTime.of(21, 0))
        .saturdayOpen(LocalTime.of(10, 0))
        .saturdayClose(LocalTime.of(17, 0))
        .holidayClosed(true)
        .build();
  }

  private Center centerWith(OperatingHours schedule, boolean isActive) {
    return Center.builder()
        .name("테스트센터")
        .region("수원시")
        .isActive(isActive)
        .schedule(schedule)
        .build();
  }

  @Test
  void TC01_평일_15시_운영중() {
    // 2026-07-14 화요일 15:00
    LocalDateTime now = LocalDateTime.of(2026, 7, 14, 15, 0);
    assertThat(defaultSchedule().isOpenAt(now, false)).isTrue();
  }

  @Test
  void TC02_평일_22시_종료() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 14, 22, 0);
    assertThat(defaultSchedule().isOpenAt(now, false)).isFalse();
  }

  @Test
  void TC03_토요일_12시_운영중() {
    // 2026-07-18 토요일 12:00
    LocalDateTime now = LocalDateTime.of(2026, 7, 18, 12, 0);
    assertThat(defaultSchedule().isOpenAt(now, false)).isTrue();
  }

  @Test
  void TC04_토요일_18시_종료() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 18, 18, 0);
    assertThat(defaultSchedule().isOpenAt(now, false)).isFalse();
  }

  @Test
  void TC05_일요일_15시_미운영() {
    // 2026-07-19 일요일 15:00
    LocalDateTime now = LocalDateTime.of(2026, 7, 19, 15, 0);
    assertThat(defaultSchedule().isOpenAt(now, false)).isFalse();
  }

  @Test
  void TC06_공휴일_평일_15시_holidayClosed_true_이면_종료() {
    LocalDateTime now = LocalDateTime.of(2026, 1, 1, 15, 0);
    assertThat(defaultSchedule().isOpenAt(now, true)).isFalse();
  }

  @Test
  void TC06b_공휴일이라도_holidayClosed_false_면_평일_로직대로() {
    OperatingHours s =
        OperatingHours.builder()
            .weekdayOpen(LocalTime.of(10, 0))
            .weekdayClose(LocalTime.of(21, 0))
            .holidayClosed(false)
            .build();
    LocalDateTime now = LocalDateTime.of(2026, 1, 1, 15, 0); // 신정, 목요일
    assertThat(s.isOpenAt(now, true)).isTrue();
  }

  @Test
  void TC07_isActive_false_이면_시각_무관_운영종료() {
    // Center 레벨에서 isActive kill-switch 조합은 호출자 (View/Service) 책임. isCurrentlyOpen 자체는 schedule 만
    // 본다.
    Center c = centerWith(defaultSchedule(), false);
    LocalDateTime now = LocalDateTime.of(2026, 7, 14, 15, 0);
    // isCurrentlyOpen 만으로는 true (schedule 관점) — kill-switch 는 View 조합 시점
    assertThat(c.isCurrentlyOpen(now, false)).isTrue();
    // 실제 배지 판정식 (View 와 동일)
    assertThat(c.isActive() && c.isCurrentlyOpen(now, false)).isFalse();
  }

  @Test
  void TC08_schedule_null_이면_isCurrentlyOpen_항상_false() {
    Center c = centerWith(null, true);
    LocalDateTime now = LocalDateTime.of(2026, 7, 14, 15, 0);
    assertThat(c.isCurrentlyOpen(now, false)).isFalse();
    assertThat(c.hasSchedule()).isFalse();
  }

  @Test
  void TC09_평일_open_정각_경계_inclusive() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 14, 10, 0);
    assertThat(defaultSchedule().isOpenAt(now, false)).isTrue();
  }

  @Test
  void TC10_평일_close_정각_경계_exclusive() {
    LocalDateTime now = LocalDateTime.of(2026, 7, 14, 21, 0);
    assertThat(defaultSchedule().isOpenAt(now, false)).isFalse();
  }

  @Test
  void TC11_close_이_open_이하_면_fail_fast() {
    assertThatThrownBy(
            () ->
                OperatingHours.builder()
                    .weekdayOpen(LocalTime.of(22, 0))
                    .weekdayClose(LocalTime.of(2, 0))
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("자정 넘김");
  }

  @Test
  void TC12_close_open_같으면_fail_fast() {
    assertThatThrownBy(
            () ->
                OperatingHours.builder()
                    .weekdayOpen(LocalTime.of(10, 0))
                    .weekdayClose(LocalTime.of(10, 0))
                    .build())
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void TC13_open_close_한_쪽만_null_이면_fail_fast() {
    assertThatThrownBy(
            () ->
                OperatingHours.builder()
                    .weekdayOpen(LocalTime.of(10, 0))
                    .weekdayClose(null)
                    .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("함께");
  }

  @Test
  void TC14_now_null_방어() {
    assertThat(defaultSchedule().isOpenAt(null, false)).isFalse();
  }

  @Test
  void TC15_일요일_운영_스케줄_확인() {
    OperatingHours s =
        OperatingHours.builder()
            .weekdayOpen(LocalTime.of(10, 0))
            .weekdayClose(LocalTime.of(21, 0))
            .sundayOpen(LocalTime.of(9, 0))
            .sundayClose(LocalTime.of(18, 0))
            .holidayClosed(true)
            .build();
    LocalDateTime sun = LocalDateTime.of(2026, 7, 19, 12, 0);
    assertThat(s.isOpenAt(sun, false)).isTrue();
  }
}
