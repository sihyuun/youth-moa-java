package io.github.sihyuuun.youthmoa.user;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class UserPrincipal implements UserDetails {

  private final Long id;
  private final String email;
  private final String displayName;
  private final String password;
  private final List<GrantedAuthority> authorities;

  /**
   * A5 admin-users (2026-09-15): 차단 상태 스냅샷. V16 이전에는 항상 true (하드코딩). 이후 {@link User#isActive()} 값을
   * 복사해 {@link #isEnabled()} 가 반영. UserDetailsService 가 매 요청마다 재로드하므로 세션 캐시가 stale 이어도 다음 요청에는 최신
   * 값이 반영됨.
   */
  private final boolean active;

  public UserPrincipal(User user) {
    this.id = user.getId();
    this.email = user.getEmail();
    this.displayName = user.getName();
    this.password = user.getPassword();
    this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    this.active = user.isActive();
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    // A5 admin-users: 차단된 유저는 DaoAuthenticationProvider 가 자동으로 DisabledException 발생 →
    // 로그인 페이지로 리다이렉트. V16 default TRUE 로 기존 유저 회귀 없음.
    return active;
  }
}
