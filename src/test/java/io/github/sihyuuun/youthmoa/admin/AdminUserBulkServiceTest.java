package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * A8 admin-bulk-csv (2026-09-17 · Qn-1 A · Qn-8 A): {@link AdminUserBulkService} 검증.
 *
 * <p>주의 — per-row REQUIRES_NEW 트랜잭션을 실증하기 위해 클래스 레벨 `@Transactional` 을 걸지 않는다. 각 테스트 후 상태는 직접 정리
 * (사용한 seed 사용자를 재활성화).
 */
@SpringBootTest
@ActiveProfiles("e2e")
class AdminUserBulkServiceTest {

  @Autowired AdminUserBulkService bulkService;
  @Autowired UserRepository userRepository;

  private User sysadmin;

  @BeforeEach
  void setup() {
    sysadmin = userRepository.findByEmail("sysadmin@youth-moa.test").orElseThrow();
    // seed1~5 를 활성 상태로 원복 (직전 테스트 오염 정리)
    for (int i = 1; i <= 5; i++) {
      final int idx = i;
      userRepository
          .findByEmail("seed" + idx + "@youth-moa.test")
          .ifPresent(
              u -> {
                if (!u.isActive()) {
                  u.reactivate();
                  userRepository.save(u);
                }
                if (u.getRole() != UserRole.USER) {
                  u.promoteTo(UserRole.USER);
                  userRepository.save(u);
                }
              });
    }
  }

  @Test
  void bulkDeactivate_reason_required() {
    List<Long> ids = List.of(seedId(1));
    assertThatThrownBy(() -> bulkService.bulkDeactivate(ids, sysadmin, ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("사유");
    // 무처리 상태 확인
    assertThat(userRepository.findById(seedId(1)).orElseThrow().isActive()).isTrue();
  }

  @Test
  void bulkDeactivate_multiple_success_and_partial_skip() {
    Long selfId = sysadmin.getId();
    Long s1 = seedId(2);
    Long s2 = seedId(3);
    // 자기 자신 포함 → 그 row 는 실패, 나머지는 성공
    BulkResult result = bulkService.bulkDeactivate(List.of(selfId, s1, s2), sysadmin, "테스트 사유");
    assertThat(result.getTotal()).isEqualTo(3);
    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getFailCount()).isEqualTo(1);
    assertThat(result.getErrors()).extracting(BulkResult.FailureRow::id).contains(selfId);
    assertThat(userRepository.findById(s1).orElseThrow().isActive()).isFalse();
    assertThat(userRepository.findById(s2).orElseThrow().isActive()).isFalse();
    // cleanup
    reactivate(s1);
    reactivate(s2);
  }

  @Test
  void bulkReactivate_idempotent_and_self_blocked() {
    Long s1 = seedId(4);
    // 먼저 차단
    bulkService.bulkDeactivate(List.of(s1), sysadmin, "테스트");
    BulkResult result = bulkService.bulkReactivate(List.of(sysadmin.getId(), s1), sysadmin);
    assertThat(result.getSuccessCount()).isEqualTo(1); // s1 만
    assertThat(result.getFailCount()).isEqualTo(1); // self
    assertThat(userRepository.findById(s1).orElseThrow().isActive()).isTrue();
  }

  @Test
  void bulkChangeRole_last_sysadmin_demote_blocked() {
    // sysadmin 1명 뿐이므로 자기 자신 포함 여부와 무관하게 demote 실패
    Long selfId = sysadmin.getId();
    BulkResult result = bulkService.bulkChangeRole(List.of(selfId), sysadmin, UserRole.USER);
    assertThat(result.getFailCount()).isEqualTo(1);
    assertThat(result.getSuccessCount()).isEqualTo(0);
  }

  @Test
  void bulkChangeRole_promote_and_check_safeguard_reuse() {
    Long s1 = seedId(5);
    BulkResult result = bulkService.bulkChangeRole(List.of(s1), sysadmin, UserRole.CENTER_ADMIN);
    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(userRepository.findById(s1).orElseThrow().getRole())
        .isEqualTo(UserRole.CENTER_ADMIN);
    // cleanup: 원상 복구
    User target = userRepository.findById(s1).orElseThrow();
    target.promoteTo(UserRole.USER);
    userRepository.save(target);
  }

  private Long seedId(int idx) {
    return userRepository.findByEmail("seed" + idx + "@youth-moa.test").orElseThrow().getId();
  }

  private void reactivate(Long id) {
    User u = userRepository.findById(id).orElseThrow();
    if (!u.isActive()) {
      u.reactivate();
      userRepository.save(u);
    }
  }
}
