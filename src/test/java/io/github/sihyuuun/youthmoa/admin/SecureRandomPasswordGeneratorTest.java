package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * A5-1 admin-staff-management (2026-09-18): {@link SecureRandomPasswordGenerator} 정책 검증.
 *
 * <p>QA + Qn-2 A: 12자 · 4문자 클래스 각 최소 1자 · 예측 불가. 100회 반복 시 중복 없음 (엔트로피 실증).
 */
class SecureRandomPasswordGeneratorTest {

  private final SecureRandomPasswordGenerator gen = new SecureRandomPasswordGenerator();

  @Test
  void generate_length_12() {
    for (int i = 0; i < 100; i++) {
      assertThat(gen.generate()).hasSize(12);
    }
  }

  @Test
  void generate_contains_all_four_classes() {
    for (int i = 0; i < 100; i++) {
      String pw = gen.generate();
      assertThat(pw).matches(".*[A-Z].*").matches(".*[a-z].*").matches(".*[0-9].*");
      assertThat(pw.chars().anyMatch(c -> SecureRandomPasswordGenerator.SPECIAL.indexOf(c) >= 0))
          .isTrue();
    }
  }

  @Test
  void generate_100_times_no_duplicates() {
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < 100; i++) seen.add(gen.generate());
    assertThat(seen).hasSize(100);
  }
}
