package io.github.sihyuuun.youthmoa.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * P0-2 회귀 방어: {@link DataInitializer#seedAdmins()} 결과 확인.
 *
 * <ul>
 *   <li>sysadmin@youth-moa.test 는 SYSTEM_ADMIN, center=null
 *   <li>center1@youth-moa.test / center2@youth-moa.test 는 CENTER_ADMIN, center + centerScope 세팅
 * </ul>
 *
 * 재기동 멱등성은 existsByEmail 체크로 보장되므로 여기서는 시드 결과 시그니처만 확인.
 */
@SpringBootTest
@ActiveProfiles("e2e")
class AdminSeedInitializerTest {

  @Autowired UserRepository userRepository;

  @Test
  void SYSTEM_ADMIN_시드_확인() {
    Optional<User> found = userRepository.findByEmail("sysadmin@youth-moa.test");
    assertThat(found).isPresent();
    User u = found.get();
    assertThat(u.getRole()).isEqualTo(UserRole.SYSTEM_ADMIN);
    assertThat(u.getCenter()).isNull();
    assertThat(u.getCenterScope()).isNull();
    assertThat(u.getName()).isEqualTo("시스템관리자");
  }

  @Test
  void CENTER_ADMIN_1_시드_확인() {
    Optional<User> found = userRepository.findByEmail("center1@youth-moa.test");
    assertThat(found).isPresent();
    User u = found.get();
    assertThat(u.getRole()).isEqualTo(UserRole.CENTER_ADMIN);
    assertThat(u.getCenter()).isNotNull();
    assertThat(u.getCenterScope()).isNotBlank();
  }

  @Test
  void CENTER_ADMIN_2_시드_확인() {
    Optional<User> found = userRepository.findByEmail("center2@youth-moa.test");
    assertThat(found).isPresent();
    User u = found.get();
    assertThat(u.getRole()).isEqualTo(UserRole.CENTER_ADMIN);
    assertThat(u.getCenter()).isNotNull();
    assertThat(u.getCenterScope()).isNotBlank();
  }

  /**
   * A7-e2e-suite 사전 시드 (2026-09-28): admin_center2 는 centers[2] 소속 CENTER_ADMIN. center1/center2
   * 시드와 다른 센터에 배치되어 organization 축과 createdBy/watcher 축 분리 검증에 사용된다.
   */
  @Test
  void ADMIN_CENTER2_시드_확인() {
    Optional<User> found = userRepository.findByEmail("admin_center2@youth-moa.test");
    assertThat(found).isPresent();
    User u = found.get();
    assertThat(u.getRole()).isEqualTo(UserRole.CENTER_ADMIN);
    assertThat(u.getCenter()).isNotNull();
    assertThat(u.getCenterScope()).isNotBlank();
    assertThat(u.isActive()).isTrue();
  }

  /**
   * A7-e2e-suite 사전 시드 (2026-09-28): admin_inactive 는 CENTER_ADMIN 이지만 isActive=false. 알림 fan-out
   * 단계에서 비활성 admin 을 skip 하는 로직 검증에 사용된다.
   */
  @Test
  void ADMIN_INACTIVE_시드_확인() {
    Optional<User> found = userRepository.findByEmail("admin_inactive@youth-moa.test");
    assertThat(found).isPresent();
    User u = found.get();
    assertThat(u.getRole()).isEqualTo(UserRole.CENTER_ADMIN);
    assertThat(u.getCenter()).isNotNull();
    assertThat(u.getCenterScope()).isNotBlank();
    assertThat(u.isActive()).isFalse();
  }
}
