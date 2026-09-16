package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AgeBucketTest {

  private static final LocalDate TODAY = LocalDate.of(2026, 9, 16);

  @Test
  void nullBirthDate_returnsUnknown() {
    assertThat(AgeBucket.of(null, TODAY)).isEqualTo(AgeBucket.UNKNOWN);
  }

  @Test
  void futureBirthDate_returnsUnknown() {
    assertThat(AgeBucket.of(LocalDate.of(2030, 1, 1), TODAY)).isEqualTo(AgeBucket.UNKNOWN);
  }

  @Test
  void under19_returnsUnknown() {
    // 만 18세 → UNKNOWN
    assertThat(AgeBucket.of(LocalDate.of(2008, 9, 16), TODAY)).isEqualTo(AgeBucket.UNKNOWN);
  }

  @Test
  void age19_boundary() {
    assertThat(AgeBucket.of(LocalDate.of(2007, 9, 16), TODAY)).isEqualTo(AgeBucket.AGE_19_24);
  }

  @Test
  void age24_boundary() {
    assertThat(AgeBucket.of(LocalDate.of(2002, 9, 16), TODAY)).isEqualTo(AgeBucket.AGE_19_24);
  }

  @Test
  void age25_boundary() {
    assertThat(AgeBucket.of(LocalDate.of(2001, 9, 16), TODAY)).isEqualTo(AgeBucket.AGE_25_29);
  }

  @Test
  void age30() {
    assertThat(AgeBucket.of(LocalDate.of(1996, 9, 16), TODAY)).isEqualTo(AgeBucket.AGE_30_34);
  }

  @Test
  void age35() {
    assertThat(AgeBucket.of(LocalDate.of(1991, 9, 16), TODAY)).isEqualTo(AgeBucket.AGE_35_39);
  }

  @Test
  void age40Plus() {
    assertThat(AgeBucket.of(LocalDate.of(1986, 9, 16), TODAY)).isEqualTo(AgeBucket.AGE_40_PLUS);
    assertThat(AgeBucket.of(LocalDate.of(1950, 1, 1), TODAY)).isEqualTo(AgeBucket.AGE_40_PLUS);
  }

  @Test
  void birthdayNotYetReached_treatsAsPreviousYear() {
    // 오늘 2026-09-16 · 생일 1996-09-17 → 아직 30세 미도래 (만 29)
    assertThat(AgeBucket.of(LocalDate.of(1996, 9, 17), TODAY)).isEqualTo(AgeBucket.AGE_25_29);
  }
}
