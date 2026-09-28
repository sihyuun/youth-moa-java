package io.github.sihyuuun.youthmoa.admin;

import io.github.sihyuuun.youthmoa.user.User;
import io.github.sihyuuun.youthmoa.user.UserPrincipal;
import io.github.sihyuuun.youthmoa.user.UserRepository;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * A7-createdBy-recipient (2026-09-28) — admin service 통합 테스트용 인증 컨텍스트 세팅 헬퍼.
 *
 * <p>{@link AdminProgramService#create} 가 {@link AdminScope#currentUser()} 로 {@link
 * io.github.sihyuuun.youthmoa.program.Program#getCreatedBy()} 를 주입하므로 {@code @SpringBootTest} 통합
 * 테스트에서 SecurityContext 를 시드 관리자로 채워야 create 경로가 통과한다.
 *
 * <p>사용 예시:
 *
 * <pre>{@code
 * @BeforeEach void auth() { AdminAuthTestSupport.loginAsSysAdmin(userRepository); }
 * @AfterEach void clear() { SecurityContextHolder.clearContext(); }
 * }</pre>
 */
public final class AdminAuthTestSupport {

  private AdminAuthTestSupport() {}

  public static User loginAsSysAdmin(UserRepository userRepository) {
    User sysadmin =
        userRepository
            .findByEmail("sysadmin@youth-moa.test")
            .orElseThrow(() -> new IllegalStateException("sysadmin seed 누락 — DataInitializer 확인"));
    UserPrincipal principal = new UserPrincipal(sysadmin);
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + sysadmin.getRole().name())));
    SecurityContextHolder.getContext().setAuthentication(auth);
    return sysadmin;
  }
}
