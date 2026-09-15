package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.application.Application;
import io.github.sihyuuun.youthmoa.application.ApplicationRepository;
import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * A5 admin-users (2026-09-15): AdminUserService 단위 검증.
 *
 * <p>커버 범위 (spec §6 정적 요구):
 *
 * <ul>
 *   <li>목록 검색 (이름·이메일 LIKE)
 *   <li>role filter 4옵션 + ALL
 *   <li>페이지네이션 10건 (PAGE_SIZE 상수)
 *   <li>차단 (사유 필수 · 자기 자신 X · 마지막 SYSTEM_ADMIN X · idempotent)
 *   <li>재활성화 (자기 자신 X · idempotent)
 *   <li>role 변경 (자기 자신 X · 마지막 SYSTEM_ADMIN 강등 X · idempotent)
 *   <li>admin-note 저장 (1000자 초과 거부 · null clear)
 *   <li>탭별 신청 이력 필터
 * </ul>
 *
 * <p>Safeguard ② (마지막 SYSTEM_ADMIN) 검증을 위해 각 시나리오는 필요 시 SYSTEM_ADMIN 을 2명 이상 시드한다 — seed 시점의
 * sysadmin 1명 만 있으면 self-check 에 먼저 걸려 safeguard ② 를 실증하지 못하기 때문. 이 사고는 curl 로 재현 불가한 실측 조건이라 반드시
 * 자동화 필요.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@Transactional
class AdminUserServiceTest {

  @Autowired AdminUserService adminUserService;
  @Autowired UserRepository userRepository;
  @Autowired ApplicationRepository applicationRepository;

  private User sysadmin; // sysadmin@youth-moa.test (시드)
  private User seed1; // seed1@youth-moa.test (시드)

  @BeforeEach
  void setup() {
    sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    seed1 = userRepository.findByEmail("seed1@youth-moa.test").orElseThrow();
  }

  // ================= 목록 · 검색 · 필터 =================

  @Test
  void list_page_size_10() {
    var page = adminUserService.list(null, null, 0);
    assertThat(page.getSize()).isEqualTo(AdminUserService.PAGE_SIZE);
    assertThat(AdminUserService.PAGE_SIZE).isEqualTo(10);
  }

  @Test
  void list_default_shows_all_roles() {
    var page = adminUserService.list(null, null, 0);
    // 시드에 SYSTEM_ADMIN 1 + CENTER_ADMIN 2 + USER seed1~30 이 있어 30건 이상
    assertThat(page.getTotalElements()).isGreaterThan(1);
  }

  @Test
  void list_role_filter_SYSTEM_ADMIN_only() {
    var page = adminUserService.list(null, "SYSTEM_ADMIN", 0);
    assertThat(page.getContent()).allMatch(u -> u.getRole() == UserRole.SYSTEM_ADMIN);
  }

  @Test
  void list_role_filter_CENTER_ADMIN_only() {
    var page = adminUserService.list(null, "CENTER_ADMIN", 0);
    assertThat(page.getContent()).isNotEmpty();
    assertThat(page.getContent()).allMatch(u -> u.getRole() == UserRole.CENTER_ADMIN);
  }

  @Test
  void list_role_filter_USER_only() {
    var page = adminUserService.list(null, "USER", 0);
    assertThat(page.getContent()).allMatch(u -> u.getRole() == UserRole.USER);
  }

  @Test
  void list_role_filter_ALL_or_invalid_ignored() {
    var all = adminUserService.list(null, "ALL", 0);
    var invalid = adminUserService.list(null, "NOT_A_ROLE", 0);
    var none = adminUserService.list(null, null, 0);
    assertThat(all.getTotalElements()).isEqualTo(none.getTotalElements());
    assertThat(invalid.getTotalElements()).isEqualTo(none.getTotalElements());
  }

  @Test
  void list_search_by_email() {
    var page = adminUserService.list("sysadmin", null, 0);
    assertThat(page.getContent()).anyMatch(u -> u.getEmail().contains("sysadmin"));
  }

  @Test
  void list_search_by_name() {
    var page = adminUserService.list("시스템관리자", null, 0);
    assertThat(page.getContent()).anyMatch(u -> u.getName().equals("시스템관리자"));
  }

  @Test
  void list_search_case_insensitive() {
    var page = adminUserService.list("SYSADMIN", null, 0);
    assertThat(page.getContent()).anyMatch(u -> u.getEmail().contains("sysadmin"));
  }

  @Test
  void list_search_no_match_returns_empty() {
    var page = adminUserService.list("__NEVER_MATCH_XYZ__", null, 0);
    assertThat(page.getContent()).isEmpty();
  }

  // ================= 상세 · 신청 이력 탭 =================

  @Test
  void findById_existing() {
    User u = adminUserService.findById(seed1.getId());
    assertThat(u.getEmail()).isEqualTo("seed1@youth-moa.test");
  }

  @Test
  void findById_missing_throws() {
    assertThatThrownBy(() -> adminUserService.findById(999_999L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("사용자를 찾을 수 없어요");
  }

  @Test
  void findApplicationsByUser_all_tab_returns_full_list() {
    List<Application> all = adminUserService.findApplicationsByUser(seed1, "ALL");
    List<Application> none = adminUserService.findApplicationsByUser(seed1, null);
    assertThat(all).hasSameSizeAs(none);
  }

  @Test
  void findApplicationsByUser_status_filter_APPROVED() {
    List<Application> approved = adminUserService.findApplicationsByUser(seed1, "APPROVED");
    // seed1 은 시드상 programs[0] APPROVED 를 갖는다 (helpers.ts 규약)
    assertThat(approved).allMatch(a -> a.getStatus().name().equals("APPROVED"));
  }

  @Test
  void findApplicationsByUser_invalid_status_returns_all() {
    List<Application> invalid = adminUserService.findApplicationsByUser(seed1, "NOT_A_STATUS");
    List<Application> none = adminUserService.findApplicationsByUser(seed1, null);
    assertThat(invalid).hasSameSizeAs(none);
  }

  // ================= 차단 =================

  @Test
  void deactivate_reason_blank_rejected() {
    assertThatThrownBy(() -> adminUserService.deactivate(seed1.getId(), sysadmin, ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("차단 사유");
  }

  @Test
  void deactivate_reason_null_rejected() {
    assertThatThrownBy(() -> adminUserService.deactivate(seed1.getId(), sysadmin, null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("차단 사유");
  }

  @Test
  void deactivate_success_persists_audit_columns() {
    adminUserService.deactivate(seed1.getId(), sysadmin, "테스트 차단 사유");

    User reloaded = userRepository.findById(seed1.getId()).orElseThrow();
    assertThat(reloaded.isActive()).isFalse();
    assertThat(reloaded.getDeactivatedAt()).isNotNull();
    assertThat(reloaded.getDeactivatedBy().getId()).isEqualTo(sysadmin.getId());
    assertThat(reloaded.getDeactivationReason()).isEqualTo("테스트 차단 사유");
  }

  @Test
  void deactivate_self_rejected_safeguard1() {
    // Safeguard 1: 자기 자신 X
    assertThatThrownBy(() -> adminUserService.deactivate(sysadmin.getId(), sysadmin, "사유"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("본인 계정");
  }

  @Test
  void deactivate_last_system_admin_rejected_safeguard2() {
    // Safeguard 2: 마지막 SYSTEM_ADMIN 은 차단 불가.
    // 시드에는 SYSTEM_ADMIN 이 sysadmin 하나만 있다. 이를 차단하려면 다른 관리자 (센터 관리자 currentAdmin) 로 시도해야
    // self-check 를 우회하고 마지막 SYSTEM_ADMIN 방어를 실증할 수 있다.
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();

    assertThatThrownBy(() -> adminUserService.deactivate(sysadmin.getId(), center1, "사유"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("마지막 시스템 관리자");
  }

  @Test
  void deactivate_not_last_system_admin_allowed() {
    // 사전에 SYSTEM_ADMIN 을 추가로 시드 (2명 이상 확보) → 마지막 SYSTEM_ADMIN safeguard 통과.
    User extraSysAdmin = seedExtraSystemAdmin();
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();

    adminUserService.deactivate(sysadmin.getId(), center1, "사유");

    User reloaded = userRepository.findById(sysadmin.getId()).orElseThrow();
    assertThat(reloaded.isActive()).isFalse();
    // extraSysAdmin 은 활성 유지
    assertThat(userRepository.findById(extraSysAdmin.getId()).orElseThrow().isActive()).isTrue();
  }

  @Test
  void deactivate_idempotent_already_inactive() {
    adminUserService.deactivate(seed1.getId(), sysadmin, "1차 사유");
    // 두 번째 호출도 예외 없이 진행 (idempotent)
    adminUserService.deactivate(seed1.getId(), sysadmin, "2차 사유");
    User reloaded = userRepository.findById(seed1.getId()).orElseThrow();
    // 첫 사유 유지 (감사 이력)
    assertThat(reloaded.getDeactivationReason()).isEqualTo("1차 사유");
  }

  // ================= 재활성화 =================

  @Test
  void reactivate_success_restores_active() {
    adminUserService.deactivate(seed1.getId(), sysadmin, "사유");
    adminUserService.reactivate(seed1.getId(), sysadmin);

    User reloaded = userRepository.findById(seed1.getId()).orElseThrow();
    assertThat(reloaded.isActive()).isTrue();
    // 감사 컬럼은 이력 보존 (spec §3 도메인 메서드 주석)
    assertThat(reloaded.getDeactivatedAt()).isNotNull();
    assertThat(reloaded.getDeactivationReason()).isEqualTo("사유");
  }

  @Test
  void reactivate_self_rejected() {
    assertThatThrownBy(() -> adminUserService.reactivate(sysadmin.getId(), sysadmin))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("본인");
  }

  @Test
  void reactivate_idempotent_already_active() {
    // seed1 은 활성 상태에서 재활성화 시도 → no-op
    adminUserService.reactivate(seed1.getId(), sysadmin);
    assertThat(userRepository.findById(seed1.getId()).orElseThrow().isActive()).isTrue();
  }

  // ================= role 변경 =================

  @Test
  void changeRole_success_USER_to_CENTER_ADMIN() {
    adminUserService.changeRole(seed1.getId(), sysadmin, UserRole.CENTER_ADMIN);
    assertThat(userRepository.findById(seed1.getId()).orElseThrow().getRole())
        .isEqualTo(UserRole.CENTER_ADMIN);
  }

  @Test
  void changeRole_success_promote_to_SYSTEM_ADMIN() {
    adminUserService.changeRole(seed1.getId(), sysadmin, UserRole.SYSTEM_ADMIN);
    assertThat(userRepository.findById(seed1.getId()).orElseThrow().getRole())
        .isEqualTo(UserRole.SYSTEM_ADMIN);
  }

  @Test
  void changeRole_null_rejected() {
    assertThatThrownBy(() -> adminUserService.changeRole(seed1.getId(), sysadmin, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void changeRole_self_rejected_safeguard1() {
    // Safeguard 1: 자기 자신 role 변경 불가
    assertThatThrownBy(() -> adminUserService.changeRole(sysadmin.getId(), sysadmin, UserRole.USER))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("본인");
  }

  @Test
  void changeRole_last_system_admin_demote_rejected_safeguard2() {
    // Safeguard 2: 마지막 SYSTEM_ADMIN 강등 불가 — 시드에는 sysadmin 만 SYSTEM_ADMIN.
    // center1 이 sysadmin 을 USER 로 강등 시도 → 마지막 SYSTEM_ADMIN 방어 걸림.
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();

    assertThatThrownBy(() -> adminUserService.changeRole(sysadmin.getId(), center1, UserRole.USER))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("마지막 시스템 관리자");
  }

  @Test
  void changeRole_not_last_system_admin_demote_allowed() {
    // 2명 이상일 때 한 명 강등 허용.
    seedExtraSystemAdmin();
    User center1 = userRepository.findByEmail("center1@youth-moa.test").orElseThrow();

    adminUserService.changeRole(sysadmin.getId(), center1, UserRole.CENTER_ADMIN);
    assertThat(userRepository.findById(sysadmin.getId()).orElseThrow().getRole())
        .isEqualTo(UserRole.CENTER_ADMIN);
  }

  @Test
  void changeRole_idempotent_same_role() {
    UserRole before = seed1.getRole();
    adminUserService.changeRole(seed1.getId(), sysadmin, before);
    assertThat(userRepository.findById(seed1.getId()).orElseThrow().getRole()).isEqualTo(before);
  }

  // ================= admin-note =================

  @Test
  void updateAdminNote_saves_value() {
    adminUserService.updateAdminNote(seed1.getId(), "테스트 메모");
    assertThat(userRepository.findById(seed1.getId()).orElseThrow().getAdminNote())
        .isEqualTo("테스트 메모");
  }

  @Test
  void updateAdminNote_null_clears_value() {
    adminUserService.updateAdminNote(seed1.getId(), "초기 메모");
    adminUserService.updateAdminNote(seed1.getId(), null);
    assertThat(userRepository.findById(seed1.getId()).orElseThrow().getAdminNote()).isNull();
  }

  @Test
  void updateAdminNote_over_1000_chars_rejected() {
    String tooLong = "가".repeat(1001);
    assertThatThrownBy(() -> adminUserService.updateAdminNote(seed1.getId(), tooLong))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("1000자");
  }

  @Test
  void updateAdminNote_exactly_1000_chars_ok() {
    String exact = "가".repeat(1000);
    adminUserService.updateAdminNote(seed1.getId(), exact);
    assertThat(userRepository.findById(seed1.getId()).orElseThrow().getAdminNote())
        .isEqualTo(exact);
  }

  // ================= role 옵션 · 라벨 =================

  @Test
  void roleOptions_returns_three_roles_in_order() {
    List<UserRole> options = adminUserService.roleOptions();
    assertThat(options)
        .containsExactly(UserRole.SYSTEM_ADMIN, UserRole.CENTER_ADMIN, UserRole.USER);
  }

  @Test
  void roleLabel_mapping() {
    assertThat(AdminUserService.roleLabel(UserRole.SYSTEM_ADMIN)).isEqualTo("시스템 관리자");
    assertThat(AdminUserService.roleLabel(UserRole.CENTER_ADMIN)).isEqualTo("관리자");
    assertThat(AdminUserService.roleLabel(UserRole.ADMIN)).isEqualTo("관리자");
    assertThat(AdminUserService.roleLabel(UserRole.USER)).isEqualTo("사용자");
    assertThat(AdminUserService.roleLabel(null)).isEqualTo("");
  }

  // ================= helper =================

  /** Safeguard ② 검증용: SYSTEM_ADMIN 이 2명 이상인 상태를 만든다. */
  private User seedExtraSystemAdmin() {
    User extra =
        User.builder()
            .email("sysadmin2@youth-moa.test")
            .password("$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx")
            .name("시스템관리자2")
            .role(UserRole.SYSTEM_ADMIN)
            .build();
    try {
      Field f = User.class.getDeclaredField("isActive");
      f.setAccessible(true);
      f.setBoolean(extra, true);
    } catch (Exception ignore) {
      // isActive 는 이미 default true — Builder 가 필드 초기값 유지
    }
    return userRepository.save(extra);
  }
}
