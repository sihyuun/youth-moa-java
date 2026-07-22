package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.github.sihyuuun.youthmoa.application.ApplicationStatus;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * MyPageController 의 필터 헬퍼 (periodCutoff · mapStatusFilter) 단위 테스트.
 *
 * <p>ym-verify T-1 잔여 항목 반영. package-private 접근으로 헬퍼 직접 호출.
 */
class MyPageControllerFilterTest {

  // ─── periodCutoff ────────────────────────────────────────────────

  @Test
  @DisplayName("periodCutoff: 3M → 90일 전")
  void periodCutoff_3M() {
    LocalDateTime cutoff = MyPageController.periodCutoff("3M");
    long days = ChronoUnit.DAYS.between(cutoff, LocalDateTime.now());
    assertThat(days).isEqualTo(90);
  }

  @Test
  @DisplayName("periodCutoff: 6M → 180일 전")
  void periodCutoff_6M() {
    LocalDateTime cutoff = MyPageController.periodCutoff("6M");
    long days = ChronoUnit.DAYS.between(cutoff, LocalDateTime.now());
    assertThat(days).isEqualTo(180);
  }

  @Test
  @DisplayName("periodCutoff: 1Y → 365일 전")
  void periodCutoff_1Y() {
    LocalDateTime cutoff = MyPageController.periodCutoff("1Y");
    long days = ChronoUnit.DAYS.between(cutoff, LocalDateTime.now());
    assertThat(days).isEqualTo(365);
  }

  @Test
  @DisplayName("periodCutoff: 3Y → 3*365일 전")
  void periodCutoff_3Y() {
    LocalDateTime cutoff = MyPageController.periodCutoff("3Y");
    long days = ChronoUnit.DAYS.between(cutoff, LocalDateTime.now());
    assertThat(days).isEqualTo(365L * 3);
  }

  @Test
  @DisplayName("periodCutoff: null → 기본 3M")
  void periodCutoff_null_defaults_to_3M() {
    LocalDateTime cutoff = MyPageController.periodCutoff(null);
    long days = ChronoUnit.DAYS.between(cutoff, LocalDateTime.now());
    assertThat(days).isEqualTo(90);
  }

  @Test
  @DisplayName("periodCutoff: 지원 안 되는 코드 → 기본 3M")
  void periodCutoff_unknown_defaults_to_3M() {
    LocalDateTime cutoff = MyPageController.periodCutoff("INVALID");
    long days = ChronoUnit.DAYS.between(cutoff, LocalDateTime.now());
    assertThat(days).isEqualTo(90);
  }

  @Test
  @DisplayName("periodCutoff: 반환값이 실행 시점 기준 산정")
  void periodCutoff_relative_to_now() {
    LocalDateTime before = MyPageController.periodCutoff("3M");
    LocalDateTime now = LocalDateTime.now();
    // 90일 근사 (테스트 실행 지연 < 1초 가정)
    assertThat(before).isCloseTo(now.minusDays(90), within(1, ChronoUnit.SECONDS));
  }

  // ─── mapStatusFilter ─────────────────────────────────────────────

  @Test
  @DisplayName("mapStatusFilter: ALL → null (전체)")
  void mapStatusFilter_ALL_returns_null() {
    assertThat(MyPageController.mapStatusFilter("ALL")).isNull();
  }

  @Test
  @DisplayName("mapStatusFilter: 대소문자 무시 all → null")
  void mapStatusFilter_all_case_insensitive() {
    assertThat(MyPageController.mapStatusFilter("all")).isNull();
    assertThat(MyPageController.mapStatusFilter("All")).isNull();
  }

  @Test
  @DisplayName("mapStatusFilter: null → null")
  void mapStatusFilter_null_returns_null() {
    assertThat(MyPageController.mapStatusFilter(null)).isNull();
  }

  @Test
  @DisplayName("mapStatusFilter: APPROVED → ApplicationStatus.APPROVED")
  void mapStatusFilter_APPROVED() {
    assertThat(MyPageController.mapStatusFilter("APPROVED")).isEqualTo(ApplicationStatus.APPROVED);
  }

  @Test
  @DisplayName("mapStatusFilter: PENDING → ApplicationStatus.PENDING")
  void mapStatusFilter_PENDING() {
    assertThat(MyPageController.mapStatusFilter("PENDING")).isEqualTo(ApplicationStatus.PENDING);
  }

  @Test
  @DisplayName("mapStatusFilter: REJECTED → ApplicationStatus.REJECTED")
  void mapStatusFilter_REJECTED() {
    assertThat(MyPageController.mapStatusFilter("REJECTED")).isEqualTo(ApplicationStatus.REJECTED);
  }

  @Test
  @DisplayName("mapStatusFilter: CANCELLED → ApplicationStatus.CANCELLED")
  void mapStatusFilter_CANCELLED() {
    assertThat(MyPageController.mapStatusFilter("CANCELLED"))
        .isEqualTo(ApplicationStatus.CANCELLED);
  }

  @Test
  @DisplayName("mapStatusFilter: 존재하지 않는 enum 이름 → null (예외 흡수)")
  void mapStatusFilter_invalid_returns_null() {
    assertThat(MyPageController.mapStatusFilter("BOGUS")).isNull();
    assertThat(MyPageController.mapStatusFilter("")).isNull();
    assertThat(MyPageController.mapStatusFilter("approved")).isNull(); // 소문자는 valueOf 실패
  }
}
