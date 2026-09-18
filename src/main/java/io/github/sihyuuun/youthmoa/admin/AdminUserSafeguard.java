package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * A8 admin-bulk (2026-09-17 · Qn-8 A): 사용자 관리 Safeguard 공통 컴포넌트.
 *
 * <p>A5 개별 endpoint 와 A8 bulk endpoint 모두에서 재사용된다. 이전에는 {@link AdminUserService} 안에 privatly 정의돼
 * 개별에서만 호출됐고, bulk 도입 시 same 규칙을 다시 카피할 위험이 있어 컴포넌트로 추출.
 *
 * <p>3종 규칙:
 *
 * <ol>
 *   <li>자기 자신 대상 X (deactivate·reactivate·role 변경 모두)
 *   <li>마지막 활성 SYSTEM_ADMIN 은 차단·강등 X
 *   <li>SYSTEM_ADMIN 승격은 SYSTEM_ADMIN 만 가능 (컨트롤러 {@code @PreAuthorize} 로 이중 방어)
 * </ol>
 *
 * <p>회귀 방어: 개별 endpoint 는 컨트롤러 → Service.deactivate/reactivate/changeRole 로 진입하며 이 컴포넌트가 아직 호출되지 않는
 * 경우 A5 회귀. 반드시 개별 endpoint 도 이 컴포넌트를 사용하도록 refactor한다.
 */
@Component
@RequiredArgsConstructor
public class AdminUserSafeguard {

  private final UserRepository userRepository;

  /** (1) 자기 자신은 조작 대상이 될 수 없음. */
  public void assertNotSelf(User admin, User target, String message) {
    if (admin != null && admin.getId() != null && admin.getId().equals(target.getId())) {
      throw new IllegalStateException(message);
    }
  }

  /**
   * (2) 마지막 활성 SYSTEM_ADMIN 을 차단하거나 강등하지 못하도록. target 이 활성 SYSTEM_ADMIN 이고 전체 활성 SYSTEM_ADMIN 수가
   * 1이면 예외.
   */
  public void assertNotLastSystemAdmin(User target) {
    if (target.getRole() != UserRole.SYSTEM_ADMIN || !target.isActive()) return;
    long activeSystemAdmins = userRepository.countByRoleAndIsActiveTrue(UserRole.SYSTEM_ADMIN);
    if (activeSystemAdmins <= 1) {
      throw new IllegalStateException("마지막 시스템 관리자는 차단하거나 강등할 수 없어요.");
    }
  }

  /** 차단 사전 검증 = 자기 자신 X + 마지막 SYSTEM_ADMIN X. */
  public void assertCanDeactivate(User admin, User target) {
    assertNotSelf(admin, target, "본인 계정은 차단할 수 없어요.");
    assertNotLastSystemAdmin(target);
  }

  /** 재활성화 사전 검증 = 자기 자신 X. (SYSTEM_ADMIN 재활성화는 최소 개수 규칙에 저촉 안 됨.) */
  public void assertCanReactivate(User admin, User target) {
    assertNotSelf(admin, target, "본인 계정은 재활성화 대상이 아니에요.");
  }

  /**
   * role 변경 사전 검증 = 자기 자신 X + SYSTEM_ADMIN → 비-SYSTEM_ADMIN 강등 시 마지막이면 차단.
   *
   * <p>SYSTEM_ADMIN 승격 자체는 컨트롤러 {@code @PreAuthorize("hasRole('SYSTEM_ADMIN')")} 로 방어됨.
   */
  public void assertCanChangeRole(User admin, User target, UserRole newRole) {
    if (newRole == null) {
      throw new IllegalArgumentException("변경할 권한을 선택해주세요.");
    }
    assertNotSelf(admin, target, "본인 계정의 권한은 변경할 수 없어요.");
    if (target.getRole() == UserRole.SYSTEM_ADMIN && newRole != UserRole.SYSTEM_ADMIN) {
      assertNotLastSystemAdmin(target);
    }
  }
}
