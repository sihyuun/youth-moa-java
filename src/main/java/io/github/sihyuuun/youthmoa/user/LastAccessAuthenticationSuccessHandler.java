package io.github.sihyuuun.youthmoa.user;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * A5 admin-users (2026-09-15): 로그인 성공 시 {@code lastAccessAt} 갱신 핸들러.
 *
 * <p>회귀 방어 (최우선): 별도 트랜잭션 ({@code REQUIRES_NEW}) + try/catch 로 예외를 삼킨다. lastAccessAt 갱신 실패는 로그인 성공에
 * 절대 영향을 주지 않는다.
 *
 * <p>사용자 트랙과 관리자 트랙 두 chain 에서 공통으로 사용. {@link
 * io.github.sihyuuun.youthmoa.common.config.SecurityConfig} 의 successHandler 로 부착.
 *
 * <p>사용자 트랙만 기본 성공 URL 이 {@code /} (자동), 관리자 트랙은 {@code /admin}. 이 값은 SecurityConfig 에서 명시적으로
 * setDefaultTargetUrl 로 지정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LastAccessAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

  @Getter private final Updater updater;

  /** 성공 URL 설정용 값 주입. defaultTargetUrl 은 SecurityConfig 에서 개별 chain 이 재설정. */
  @Value("${security.default-success-url:/}")
  private String defaultSuccessUrl;

  @Override
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException, ServletException {
    try {
      if (authentication.getPrincipal() instanceof UserPrincipal up && up.getId() != null) {
        updater.updateLastAccess(up.getId());
      }
    } catch (Exception ex) {
      // 회귀 방어: 갱신 실패해도 로그인 flow 는 반드시 성공. log.warn 만 남김.
      log.warn(
          "[admin-users][A5] lastAccessAt 갱신 실패 (login flow 는 계속 진행) userPrincipal={} : {}",
          authentication.getPrincipal(),
          ex.getMessage());
    }
    super.onAuthenticationSuccess(request, response, authentication);
  }

  /** 별도 트랜잭션에서 lastAccessAt 을 갱신. Spring 프록시 자기 호출 제약을 피하기 위해 별도 컴포넌트로 분리. */
  @Component
  @RequiredArgsConstructor
  public static class Updater {
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateLastAccess(Long userId) {
      userRepository.findById(userId).ifPresent(user -> user.updateLastAccess(LocalDateTime.now()));
    }
  }
}
