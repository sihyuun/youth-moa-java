package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.common.config.JpaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * F0i: FindAccountService — @DataJpaTest (H2) 로 매칭·재설정 시나리오 검증.
 *
 * <p>휴대폰 정규화 로직도 함께 검증한다 (하이픈 입력 → 저장은 하이픈 없이 매칭 성공).
 *
 * <p>JpaConfig import 필수 — @DataJpaTest 는 기본으로 @Configuration 을 스캔하지 않아 @EnableJpaAuditing 이 안 걸림
 * → @CreatedDate 필드가 null 이 되어 NOT NULL constraint 위반. JpaConfig 를 명시적 import 해서 auditing 활성화.
 */
@DataJpaTest
@Import({JpaConfig.class, FindAccountServiceTest.Config.class})
class FindAccountServiceTest {

  @Autowired UserRepository userRepository;
  @Autowired FindAccountService findAccountService;
  @Autowired PasswordEncoder passwordEncoder;

  private User seed;

  @BeforeEach
  void setUp() {
    seed =
        userRepository.save(
            User.builder()
                .email("seeder@youth-moa.test")
                .password(passwordEncoder.encode("Old1234!"))
                .name("시드유저1")
                .phone("01000000001")
                .role(UserRole.USER)
                .build());
  }

  @Test
  void 이름_휴대폰_매칭() {
    assertThat(findAccountService.findEmailByNameAndPhone("시드유저1", "01000000001"))
        .isPresent()
        .get()
        .extracting(User::getEmail)
        .isEqualTo("seeder@youth-moa.test");
  }

  @Test
  void 휴대폰_하이픈_입력도_정규화_후_매칭() {
    assertThat(findAccountService.findEmailByNameAndPhone("시드유저1", "010-0000-0001")).isPresent();
  }

  @Test
  void 미매칭이면_empty() {
    assertThat(findAccountService.findEmailByNameAndPhone("존재하지않음", "01000000001")).isEmpty();
  }

  @Test
  void 비밀번호_재설정_후_새_비밀번호로_검증가능() {
    findAccountService.resetPassword(seed.getId(), "NewPass1234!");
    User reloaded = userRepository.findById(seed.getId()).orElseThrow();
    assertThat(passwordEncoder.matches("NewPass1234!", reloaded.getPassword())).isTrue();
    assertThat(passwordEncoder.matches("Old1234!", reloaded.getPassword())).isFalse();
  }

  static class Config {
    @org.springframework.context.annotation.Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }

    @org.springframework.context.annotation.Bean
    FindAccountService findAccountService(UserRepository repo, PasswordEncoder pe) {
      return new FindAccountService(repo, pe);
    }
  }
}
