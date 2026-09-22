package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import io.github.sihyuuun.youthmoa.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * A1 (Qn-5 A): 관리자 대시보드 조회 스코프 유틸.
 *
 * <p>SecurityContext 의 현재 사용자로부터 파생한다.
 *
 * <ul>
 *   <li>SYSTEM_ADMIN → {@code null} (전체 센터)
 *   <li>CENTER_ADMIN → 자기 소속 {@code center.name}
 *   <li>그 외 → {@code null} (컨트롤러/필터에서 이미 접근 제어됐다는 전제)
 * </ul>
 *
 * <p>A9-b (2026-09-22) 이후: Program.center FK (NOT NULL) 기반으로 격리한다. 문자열 매칭 근사는 폐기. {@link
 * #effectiveCenterId()} 를 격리 필터에 사용하고 {@link #effectiveCenterName()} 은 라벨/헤더 표시용으로만 유지.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminScope {

  private final UserRepository userRepository;

  /**
   * 현재 인증 컨텍스트에서 파생한 "유효 센터 이름". null 이면 전체(SYSTEM_ADMIN) 스코프.
   *
   * <p>User 엔티티의 center 를 세션 principal 이 보관하지 않으므로 DB 조회로 확인한다 (요청당 1회).
   */
  public String effectiveCenterName() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return null;
    Object principal = auth.getPrincipal();
    if (!(principal instanceof UserPrincipal up)) return null;
    User user = userRepository.findById(up.getId()).orElse(null);
    if (user == null) return null;
    if (user.getRole() == UserRole.SYSTEM_ADMIN) return null;
    if (user.getRole() == UserRole.CENTER_ADMIN && user.getCenter() != null) {
      return user.getCenter().getName();
    }
    return null;
  }

  /**
   * A9-a (2026-09-21): 유효 센터 ID — Program.center FK 기반 필터에 사용. null 이면 전체 스코프.
   *
   * <p>{@link #effectiveCenterName()} 은 라벨/CSV export 헤더 등 표시 용도로 유지, 실 격리 필터는 FK id 로 전환.
   */
  public Long effectiveCenterId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) return null;
    Object principal = auth.getPrincipal();
    if (!(principal instanceof UserPrincipal up)) return null;
    User user = userRepository.findById(up.getId()).orElse(null);
    if (user == null) return null;
    if (user.getRole() == UserRole.SYSTEM_ADMIN) return null;
    if (user.getRole() == UserRole.CENTER_ADMIN && user.getCenter() != null) {
      return user.getCenter().getId();
    }
    return null;
  }

  /** 현재 관리자의 표시용 센터 라벨 (SYSTEM_ADMIN 은 "전체", CENTER_ADMIN 은 센터명). */
  public String centerScopeLabel() {
    String name = effectiveCenterName();
    return name == null ? "전체" : name;
  }

  /** 현재 관리자가 SYSTEM_ADMIN 인지 여부 (헤더 배지·드롭다운 노출 판단). */
  public boolean isSystemAdmin() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return false;
    return auth.getAuthorities().stream()
        .anyMatch(a -> "ROLE_SYSTEM_ADMIN".equals(a.getAuthority()));
  }
}
