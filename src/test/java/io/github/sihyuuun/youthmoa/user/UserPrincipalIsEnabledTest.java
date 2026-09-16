package io.github.sihyuuun.youthmoa.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * A5 admin-users (2026-09-15): {@link UserPrincipal#isEnabled()} 회귀 방어.
 *
 * <p>V16 이전에는 하드코딩 {@code true} 였다. 지금은 {@link User#isActive()} 값이 그대로 반영되어야 Spring Security 의
 * {@code DaoAuthenticationProvider} 가 차단된 사용자를 자동으로 {@code DisabledException} 으로 거절할 수 있다.
 *
 * <p>회귀 리스크: 이 값이 다시 하드코딩 {@code true} 로 돌아가거나, isActive 필드 mapping 이 잘못되면 차단 사용자가 로그인 가능. 그 경로가
 * broken 되면 A5 의 회원 차단 기능 자체가 무의미해지므로 이 테스트가 회귀 감시 게이트.
 *
 * <p>순수 단위 테스트 — SpringBootTest 를 붙일 필요가 없다. Reflection 으로 User 필드만 세팅해 동작 검증.
 */
class UserPrincipalIsEnabledTest {

  @Test
  void isEnabled_true_when_user_isActive_true() throws Exception {
    User active = buildActiveUser();
    setActive(active, true);

    UserPrincipal principal = new UserPrincipal(active);
    assertThat(principal.isEnabled()).isTrue();
  }

  @Test
  void isEnabled_false_when_user_isActive_false() throws Exception {
    User blocked = buildActiveUser();
    setActive(blocked, false);

    UserPrincipal principal = new UserPrincipal(blocked);
    assertThat(principal.isEnabled()).isFalse();
  }

  @Test
  void isEnabled_snapshot_captured_at_construction_time() throws Exception {
    // spec §3 확인: UserPrincipal 은 필드 스냅샷 방식. 생성 이후 User 를 mutate 해도 principal 은 변하지 않아야 한다.
    // (UserDetailsService 가 매 요청 재로드하므로 실사용 flow 는 안전.)
    User user = buildActiveUser();
    setActive(user, true);
    UserPrincipal principal = new UserPrincipal(user);

    setActive(user, false); // 원본 mutate

    // 스냅샷 값 유지 — 세션 캐시 무효화는 SecurityConfig / UserDetailsService 재로드가 담당
    assertThat(principal.isEnabled()).isTrue();
  }

  @Test
  void other_flags_return_true_by_default() {
    // A5 스코프는 isEnabled 만 반영. 나머지 세 flag 는 true 유지.
    User u = buildActiveUser();
    UserPrincipal p = new UserPrincipal(u);
    assertThat(p.isAccountNonExpired()).isTrue();
    assertThat(p.isAccountNonLocked()).isTrue();
    assertThat(p.isCredentialsNonExpired()).isTrue();
  }

  @Test
  void role_authority_prefixed_with_ROLE_() {
    User u = buildActiveUser();
    UserPrincipal p = new UserPrincipal(u);
    assertThat(p.getAuthorities())
        .anyMatch(a -> a.getAuthority().equals("ROLE_" + u.getRole().name()));
  }

  // ============ helpers ============

  private static User buildActiveUser() {
    return User.builder().email("test@t.com").password("x").name("이름").role(UserRole.USER).build();
  }

  private static void setActive(User user, boolean value) throws Exception {
    Field f = User.class.getDeclaredField("isActive");
    f.setAccessible(true);
    f.setBoolean(user, value);
  }
}
