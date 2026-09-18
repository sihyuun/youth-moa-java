package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * A8 admin-bulk-csv (2026-09-17 · Qn-8 A): {@link AdminUserSafeguard} 단위 검증.
 *
 * <p>Safeguard 3종 규칙이 개별·bulk 재사용 컴포넌트로 정확히 동작하는지 확인. 마지막 SYSTEM_ADMIN 케이스는 SYSTEM_ADMIN 을 여러 명 시딩해
 * 실증.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminUserSafeguardTest {

  @Autowired AdminUserSafeguard safeguard;
  @Autowired UserRepository userRepository;

  private User sysadmin;
  private User seedUser;

  @BeforeEach
  void setup() {
    sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    seedUser = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
  }

  @Test
  void assertNotSelf_throws_when_admin_equals_target() {
    assertThatThrownBy(() -> safeguard.assertNotSelf(sysadmin, sysadmin, "본인 불가"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("본인 불가");
  }

  @Test
  void assertNotSelf_passes_when_different() {
    safeguard.assertNotSelf(sysadmin, seedUser, "본인 불가");
    // no exception
  }

  @Test
  void assertNotLastSystemAdmin_blocks_when_only_one() {
    // 시드에 SYSTEM_ADMIN 은 sysadmin 1명만 있으므로 마지막 조건 성립
    assertThatThrownBy(() -> safeguard.assertNotLastSystemAdmin(sysadmin))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("마지막 시스템 관리자");
  }

  @Test
  void assertNotLastSystemAdmin_passes_when_multiple() {
    // 추가 SYSTEM_ADMIN 을 시드 (트랜잭션 내)
    userRepository.save(
        User.builder()
            .email("extra-sysadmin@test.local")
            .name("extra")
            .role(UserRole.SYSTEM_ADMIN)
            .password("noop")
            .build());
    safeguard.assertNotLastSystemAdmin(sysadmin);
  }

  @Test
  void assertCanChangeRole_null_role_rejected() {
    assertThatThrownBy(() -> safeguard.assertCanChangeRole(sysadmin, seedUser, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("권한");
  }

  @Test
  void assertCanChangeRole_self_rejected() {
    assertThatThrownBy(() -> safeguard.assertCanChangeRole(sysadmin, sysadmin, UserRole.USER))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("본인");
  }

  @Test
  void assertCanChangeRole_last_sysadmin_demote_rejected() {
    // sysadmin 이 유일 SYSTEM_ADMIN 이면 USER 로 강등 불가
    assertThatThrownBy(
            () -> safeguard.assertCanChangeRole(seedUser, sysadmin, UserRole.CENTER_ADMIN))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void assertCanChangeRole_normal_promotion_passes() {
    safeguard.assertCanChangeRole(sysadmin, seedUser, UserRole.CENTER_ADMIN);
    // no exception - actual role change 은 도메인 메서드가 담당
    assertThat(seedUser.getRole()).isEqualTo(UserRole.USER); // 여전히 원래 role
  }
}
