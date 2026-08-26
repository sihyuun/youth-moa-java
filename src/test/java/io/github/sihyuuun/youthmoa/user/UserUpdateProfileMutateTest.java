package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 260826 A#1: User.updateProfile @ElementCollection mutate 패턴 회귀 방어.
 *
 * <p>배경: F-signup-01 ym-verify PR #95 refute 로 User.updateProfile 이 interestRegions ·
 * interestCategories 를 재할당 (this.field = ...) 하던 legacy 발견. Hibernate PersistentSet 은 인스턴스 재할당 시
 * 트래킹이 끊겨 UPDATE 가 flush 되지 않을 위험. updateInterests() 는 이미 mutate 패턴 준수했지만 updateProfile 은 미처리.
 *
 * <p>이 테스트는 실제 DB flush 후 재조회 시 관심 지역·분야가 정확히 갱신됨을 검증. 재할당 방식으로 회귀하면 flush 후 재조회 시 이전 값 잔존 · 삭제된 값
 * 잔존 등으로 실패.
 */
@DataJpaTest
@Import({JpaConfig.class, UserUpdateProfileMutateTest.Config.class})
class UserUpdateProfileMutateTest {

  @Autowired UserRepository userRepository;
  @Autowired PasswordEncoder passwordEncoder;

  @Test
  @DisplayName("관심 지역·분야 mutate 방식으로 flush 후 재조회 시 정확히 갱신된다")
  void updateProfile_flushes_interest_changes() {
    User seed =
        userRepository.saveAndFlush(
            User.builder()
                .email("mutate@youth-moa.test")
                .password(passwordEncoder.encode("Test1234!"))
                .name("초기이름")
                .phone("01000000001")
                .role(UserRole.USER)
                .interestRegions(new java.util.HashSet<>(Set.of("부천시", "안산시")))
                .interestCategories(new java.util.HashSet<>(Set.of("취업", "창업")))
                .build());

    // 프로필 편집: 관심 지역·분야 부분 교체 (부천시·취업 유지, 안산시·창업 제거, 성남시·마음건강 신설)
    seed.updateProfile(
        "새이름",
        "01099999999",
        "12345",
        "경기도 부천시",
        "101호",
        java.time.LocalDate.of(1998, 3, 21),
        UserGender.FEMALE,
        Set.of("부천시", "성남시"),
        Set.of("취업", "마음건강"));
    userRepository.saveAndFlush(seed);

    // 재조회로 실 flush 검증 (mutate 아니면 UPDATE 가 flush 안 돼 원 값 잔존)
    User reloaded = userRepository.findById(seed.getId()).orElseThrow();
    assertThat(reloaded.getName()).isEqualTo("새이름");
    assertThat(reloaded.getPhone()).isEqualTo("01099999999");
    assertThat(reloaded.getInterestRegions()).containsExactlyInAnyOrder("부천시", "성남시");
    assertThat(reloaded.getInterestCategories()).containsExactlyInAnyOrder("취업", "마음건강");
  }

  @Test
  @DisplayName("관심 지역·분야 null 파라미터는 빈 셋으로 처리된다 (기존 값 완전 제거)")
  void updateProfile_null_interests_clears_all() {
    User seed =
        userRepository.saveAndFlush(
            User.builder()
                .email("null-interest@youth-moa.test")
                .password(passwordEncoder.encode("Test1234!"))
                .name("초기이름")
                .phone("01000000002")
                .role(UserRole.USER)
                .interestRegions(new java.util.HashSet<>(Set.of("수원시")))
                .interestCategories(new java.util.HashSet<>(Set.of("취업")))
                .build());

    seed.updateProfile(
        "이름",
        "01000000002",
        null,
        null,
        null,
        null,
        null,
        null, // interestRegions null
        null); // interestCategories null
    userRepository.saveAndFlush(seed);

    User reloaded = userRepository.findById(seed.getId()).orElseThrow();
    assertThat(reloaded.getInterestRegions()).isEmpty();
    assertThat(reloaded.getInterestCategories()).isEmpty();
  }

  static class Config {
    @org.springframework.context.annotation.Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }
}
