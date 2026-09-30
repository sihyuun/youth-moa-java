package io.github.sihyuuun.youthmoa.admin;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * A7 admin-invitation-mail (2026-09-30) — 실 SMTP 발송 구현체.
 *
 * <p>활성 조건: {@code youthmoa.mail.mock=false}. Spring Boot autoconfig 가 {@code spring.mail.*} 로부터
 * {@link JavaMailSender} 를 자동 구성 (host/port/auth/starttls 등).
 *
 * <p>MIME 조립 파이프라인:
 *
 * <ol>
 *   <li>{@link ITemplateEngine#process(String, org.thymeleaf.context.IContext)} 로 Thymeleaf 템플릿 →
 *       HTML String.
 *   <li>{@link MimeMessageHelper} 로 from/to/subject/body 세팅 (multipart=false, UTF-8).
 *   <li>{@link JavaMailSender#send(MimeMessage)} — 여기서 실 SMTP wire 통신 발생.
 * </ol>
 *
 * <p>예외 정책 (Q2 A fallback): {@link MailException} · {@link MessagingException} 을 catch 해 {@link
 * MailDispatchResult#failure(String)} 반환. throw 하지 않는다 — 상위(AdminUserService)는 계정 저장·password 재설정을
 * 이미 커밋했고, 컨트롤러는 이 결과로 flash 분기(성공 배너 vs 실패 배너 + password 노출)를 결정한다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "youthmoa.mail.mock", havingValue = "false")
public class SmtpAdminInvitationMailSender implements AdminInvitationMailService {

  private static final String TEMPLATE_INVITATION = "admin-invitation";
  private static final String TEMPLATE_PASSWORD_RESET = "admin-password-reset";

  private final JavaMailSender mailSender;
  private final ITemplateEngine mailTemplateEngine;
  private final AdminMailProperties mailProperties;

  public SmtpAdminInvitationMailSender(
      JavaMailSender mailSender,
      @Qualifier("mailTemplateEngine") ITemplateEngine mailTemplateEngine,
      AdminMailProperties mailProperties) {
    this.mailSender = mailSender;
    this.mailTemplateEngine = mailTemplateEngine;
    this.mailProperties = mailProperties;
  }

  @Override
  public MailDispatchResult sendInvitation(String toEmail, String toName, String tempPassword) {
    String subject = "[" + mailProperties.serviceName() + "] 관리자 계정이 발급되었어요";
    return dispatch(TEMPLATE_INVITATION, subject, toEmail, toName, tempPassword);
  }

  @Override
  public MailDispatchResult sendPasswordReset(String toEmail, String toName, String tempPassword) {
    String subject = "[" + mailProperties.serviceName() + "] 임시 비밀번호가 재발급되었어요";
    return dispatch(TEMPLATE_PASSWORD_RESET, subject, toEmail, toName, tempPassword);
  }

  private MailDispatchResult dispatch(
      String templateName, String subject, String toEmail, String toName, String tempPassword) {
    try {
      String html = renderTemplate(templateName, toEmail, toName, tempPassword);
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper =
          new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
      helper.setFrom(mailProperties.fromAddress(), mailProperties.fromName());
      helper.setTo(toEmail);
      helper.setSubject(subject);
      helper.setText(html, true); // html=true → Content-Type: text/html
      mailSender.send(message);
      log.info("[MAIL] sent template={} to={}", templateName, toEmail);
      return MailDispatchResult.success();
    } catch (MessagingException | UnsupportedEncodingException | MailException e) {
      log.error("[MAIL] send failed template={} to={} : {}", templateName, toEmail, e.getMessage());
      return MailDispatchResult.failure(shortReason(e));
    }
  }

  private String renderTemplate(
      String templateName, String toEmail, String toName, String tempPassword) {
    Context ctx = new Context();
    ctx.setVariable("email", toEmail);
    ctx.setVariable("name", toName);
    ctx.setVariable("tempPassword", tempPassword);
    ctx.setVariable("loginUrl", mailProperties.loginUrl());
    ctx.setVariable("serviceName", mailProperties.serviceName());
    return mailTemplateEngine.process(templateName, ctx);
  }

  /** SMTP 예외 → 사용자 노출 가능한 짧은 사유. 상세는 log 에만 남기고 화면엔 요약만. */
  private static String shortReason(Exception e) {
    String msg = e.getMessage();
    if (msg == null || msg.isBlank()) return "SMTP 오류";
    // 첫 줄만 사용 (stack trace · 개행 제거)
    int nl = msg.indexOf('\n');
    return nl > 0 ? msg.substring(0, nl) : msg;
  }
}
