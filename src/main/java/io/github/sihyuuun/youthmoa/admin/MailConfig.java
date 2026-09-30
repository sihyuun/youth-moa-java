package io.github.sihyuuun.youthmoa.admin;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * A7 admin-invitation-mail (2026-09-30) — Mail 도메인 전용 설정.
 *
 * <ul>
 *   <li>{@link AdminMailProperties} 를 스캔 (@ConfigurationProperties record 는 @Enable... 로 등록해야 bean
 *       등록됨).
 *   <li>{@link #mailTemplateEngine()} — 메일 전용 Thymeleaf 엔진. 웹 뷰 엔진과 분리 (prefix
 *       classpath:/templates/mail/, SpringSecurityDialect · WebContext 미필요, HTML mode). Spring Boot
 *       기본 {@code templateEngine} 을 그대로 쓰면 mail-specific resolver 를 얹기 어렵고 캐시 정책도 웹과 뒤섞인다.
 * </ul>
 *
 * <p>왜 별도 TemplateEngine 인가: JavaMailSender 의 MimeMessageHelper 는 String 본문을 받는다. Thymeleaf 는 view
 * 를 String 으로 render 하려면 {@code TemplateEngine.process(name, Context)} 를 호출한다. web request 컨텍스트가
 * 없어도 동작하도록 별도 엔진에 별도 resolver 를 두는 것이 표준 패턴이다.
 */
@Configuration
@EnableConfigurationProperties(AdminMailProperties.class)
public class MailConfig {

  /**
   * 메일 전용 엔진.
   *
   * <p>주의: 리턴 타입을 {@code SpringTemplateEngine} 으로 두면 Spring Boot 의 web Thymeleaf autoconfig 가 이
   * bean 을 primary 로 인식해 웹 뷰 렌더도 이 엔진(prefix templates/mail/) 을 사용하게 되어 admin/user/detail 등이 못 열린다.
   * 리턴 타입을 plain {@link ITemplateEngine} 로 노출해 autoconfig 가 자체 SpringTemplateEngine 을 별도로 만들도록
   * 유지한다. web용 SpringSecurityDialect 등도 그쪽에 알아서 붙는다.
   */
  @Bean("mailTemplateEngine")
  public ITemplateEngine mailTemplateEngine() {
    ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
    resolver.setPrefix("templates/mail/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    // 메일 템플릿은 자주 바뀌지 않으므로 캐시 on (개발 중에도 재기동으로 갱신 충분).
    resolver.setCacheable(true);

    TemplateEngine engine = new TemplateEngine();
    engine.setTemplateResolver(resolver);
    return engine;
  }
}
