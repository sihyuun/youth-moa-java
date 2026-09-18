package io.github.sihyuuun.youthmoa.common.config;

import io.github.sihyuuun.youthmoa.stats.VisitTrackingInterceptor;
import io.github.sihyuuun.youthmoa.user.PasswordChangeRequiredInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * A6 (2026-09-16 신설) — WebMvc 설정 + {@code @EnableScheduling}.
 *
 * <p>{@link VisitTrackingInterceptor} 등록. admin · 정적 리소스 · webjars · actuator · /error ·
 * login/logout POST 는 제외.
 *
 * <p>{@code @EnableScheduling} 은 {@code DailyVisitScheduler} 활성화용. 테스트는 {@code
 * spring.task.scheduling.enabled=false} 로 억제.
 */
@Configuration
@EnableScheduling
public class WebMvcConfig implements WebMvcConfigurer {

  private final ObjectProvider<VisitTrackingInterceptor> visitTrackingInterceptor;
  private final ObjectProvider<PasswordChangeRequiredInterceptor> passwordChangeInterceptor;

  public WebMvcConfig(
      ObjectProvider<VisitTrackingInterceptor> visitTrackingInterceptor,
      ObjectProvider<PasswordChangeRequiredInterceptor> passwordChangeInterceptor) {
    this.visitTrackingInterceptor = visitTrackingInterceptor;
    this.passwordChangeInterceptor = passwordChangeInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    // A5-1 admin-staff-management (2026-09-18): 강제 password 변경 인터셉터를 먼저 등록.
    // 인터셉터 내부에서 flag=FALSE 유저는 즉시 통과 (O(1)) — 기존 사용자 무회귀.
    // /password/change · /login · 정적 리소스 등은 인터셉터 내부에서 예외 처리.
    PasswordChangeRequiredInterceptor pwc = passwordChangeInterceptor.getIfAvailable();
    if (pwc != null) {
      registry.addInterceptor(pwc).addPathPatterns("/**");
    }

    VisitTrackingInterceptor interceptor = visitTrackingInterceptor.getIfAvailable();
    if (interceptor == null) {
      // @WebMvcTest 슬라이스 등 collector 미스캔 컨텍스트에서는 등록 스킵.
      return;
    }
    registry
        .addInterceptor(interceptor)
        .addPathPatterns("/**")
        .excludePathPatterns(
            "/admin/**",
            "/actuator/**",
            "/css/**",
            "/js/**",
            "/images/**",
            "/webjars/**",
            "/favicon.ico",
            "/error",
            "/login",
            "/logout",
            "/h2-console/**",
            "/__test__/**");
  }
}
