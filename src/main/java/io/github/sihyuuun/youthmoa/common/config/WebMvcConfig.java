package io.github.sihyuuun.youthmoa.common.config;

import io.github.sihyuuun.youthmoa.stats.VisitTrackingInterceptor;
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

  public WebMvcConfig(ObjectProvider<VisitTrackingInterceptor> visitTrackingInterceptor) {
    this.visitTrackingInterceptor = visitTrackingInterceptor;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
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
