package io.github.sihyuuun.youthmoa.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * A7 admin-invitation-mail (2026-09-30) — {@code youthmoa.mail.*} 프로퍼티 바인딩.
 *
 * <p>{@code @ConfigurationProperties} 는 {@code @Value} 여러 개보다 안전 · 명시적이다. 필드 하나에 프로퍼티 하나가 대응하므로 오타
 * 시 부팅 시점에 감지되고, IDE 자동완성도 가능하다 (spring-boot-configuration-processor 도입 시).
 *
 * @param mock true 이면 {@code MockAdminInvitationMailSender} 활성 (실 SMTP 미접속, 로그만).
 * @param fromAddress 발신 이메일 주소. Gmail 은 계정 주소 그대로, MailHog 은 임의 문자열 허용.
 * @param fromName 발신자 표시명 (예: "청년몽땅"). MimeMessageHelper 가 RFC 2047 로 UTF-8 인코딩.
 * @param loginUrl 메일 본문 CTA 링크. 절대 URL 필수 (mail 클라이언트는 상대 URL 해석 못 함).
 * @param serviceName 브랜드명. subject 조립에 사용 (본문·상단 wordmark 는 템플릿에 "청년모아" 하드코딩).
 * @param supportEmail 메일 footer 문의처. mailto 링크 + 표시 텍스트로 사용 (2026-09-30 사용자 템플릿 편입).
 */
@ConfigurationProperties(prefix = "youthmoa.mail")
public record AdminMailProperties(
    boolean mock,
    String fromAddress,
    String fromName,
    String loginUrl,
    String serviceName,
    String supportEmail) {}
