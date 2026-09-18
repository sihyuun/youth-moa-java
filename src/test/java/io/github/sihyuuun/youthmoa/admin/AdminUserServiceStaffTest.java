package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.center.Center;
import io.github.sihyuuun.youthmoa.center.CenterRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserGender;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * A5-1 admin-staff-management (2026-09-18): {@link AdminUserService#createStaff} · {@link
 * AdminUserService#resetPassword} 단위 검증.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminUserServiceStaffTest {

  @Autowired AdminUserService adminUserService;
  @Autowired UserRepository userRepository;
  @Autowired CenterRepository centerRepository;
  @Autowired PasswordEncoder passwordEncoder;

  private User sysadmin;
  private User seedUser;

  @BeforeEach
  void setup() {
    sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    seedUser = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
  }

  @Test
  void createStaff_user_role_generates_password_and_sets_flag() {
    AdminUserService.CreatedStaff created =
        adminUserService.createStaff(
            sysadmin, "newbie@test.local", "새사용자", UserGender.MALE, UserRole.USER, null);

    assertThat(created.plainPassword()).hasSize(12);
    User saved = userRepository.findByEmail("newbie@test.local").orElseThrow();
    assertThat(saved.getRole()).isEqualTo(UserRole.USER);
    assertThat(saved.isMustChangePassword()).isTrue();
    assertThat(saved.getInvitedBy().getId()).isEqualTo(sysadmin.getId());
    assertThat(saved.getPasswordChangedAt()).isNull();
    assertThat(passwordEncoder.matches(created.plainPassword(), saved.getPassword())).isTrue();
  }

  @Test
  void createStaff_center_admin_requires_center() {
    assertThatThrownBy(
            () ->
                adminUserService.createStaff(
                    sysadmin,
                    "ca@test.local",
                    "센터어드민",
                    UserGender.FEMALE,
                    UserRole.CENTER_ADMIN,
                    null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("소속 센터");
  }

  @Test
  void createStaff_center_admin_with_center_ok() {
    Center center = centerRepository.findAllByOrderByNameAsc().stream().findFirst().orElseThrow();
    AdminUserService.CreatedStaff created =
        adminUserService.createStaff(
            sysadmin,
            "ca2@test.local",
            "센터어드민2",
            UserGender.MALE,
            UserRole.CENTER_ADMIN,
            center.getId());
    User saved = userRepository.findByEmail("ca2@test.local").orElseThrow();
    assertThat(saved.getCenter().getId()).isEqualTo(center.getId());
    assertThat(saved.getRole()).isEqualTo(UserRole.CENTER_ADMIN);
    assertThat(created.plainPassword()).isNotBlank();
  }

  @Test
  void createStaff_duplicate_email_rejected() {
    assertThatThrownBy(
            () ->
                adminUserService.createStaff(
                    sysadmin, seedUser.getEmail(), "중복", UserGender.MALE, UserRole.USER, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("사용 중");
  }

  @Test
  void createStaff_blank_name_rejected() {
    assertThatThrownBy(
            () ->
                adminUserService.createStaff(
                    sysadmin, "blank@test.local", "  ", UserGender.MALE, UserRole.USER, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("이름");
  }

  @Test
  void createStaff_blank_email_rejected() {
    assertThatThrownBy(
            () ->
                adminUserService.createStaff(
                    sysadmin, "  ", "이름", UserGender.MALE, UserRole.USER, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("이메일");
  }

  @Test
  void createStaff_normalizes_email_lowercase_trim() {
    AdminUserService.CreatedStaff created =
        adminUserService.createStaff(
            sysadmin, "  MIXED@Test.Local ", "혼합", UserGender.MALE, UserRole.USER, null);
    assertThat(created.user().getEmail()).isEqualTo("mixed@test.local");
  }

  @Test
  void resetPassword_updates_password_and_resets_flag() {
    String oldPasswordHash = seedUser.getPassword();
    String newPlain = adminUserService.resetPassword(seedUser.getId(), sysadmin);
    User reloaded = userRepository.findById(seedUser.getId()).orElseThrow();
    assertThat(newPlain).hasSize(12);
    assertThat(reloaded.isMustChangePassword()).isTrue();
    assertThat(passwordEncoder.matches(newPlain, reloaded.getPassword())).isTrue();
    assertThat(reloaded.getPassword()).isNotEqualTo(oldPasswordHash);
  }

  @Test
  void resetPassword_self_rejected() {
    assertThatThrownBy(() -> adminUserService.resetPassword(sysadmin.getId(), sysadmin))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("본인");
  }
}
