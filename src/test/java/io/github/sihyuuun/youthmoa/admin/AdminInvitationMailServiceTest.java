package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * A7 admin-invitation-mail (2026-09-30) — GreenMail embedded SMTP 로 실 프로토콜 회귀 검증.
 *
 * <p>왜 GreenMail 인가:
 *
 * <ul>
 *   <li>Mockito 로 {@code JavaMailSender} 를 stub 하면 send() 호출 여부만 검증 가능. MIME 조립·헤더·charset 등 {@link
 *       SmtpAdminInvitationMailSender} 의 실 로직은 검증되지 않는다.
 *   <li>GreenMail 은 in-process SMTP 서버로, 실제 wire 프로토콜로 발송된 메시지를 read back 할 수 있다. 발송 파이프라인 전체 (템플릿
 *       render → MIME → SMTP) 를 회귀 방어.
 *   <li>Testcontainers Mailhog 방식과 비교: Docker 없이 pure-Java 로 동작. CI 성능·격리 우수.
 * </ul>
 *
 * <p>포트: 3025 (GreenMail 기본 SMTP dynamic offset). {@code @TestPropertySource} 로
 * spring.mail.host/port 를 localhost:3025 로 override + {@code youthmoa.mail.mock=false} 로 {@link
 * SmtpAdminInvitationMailSender} 활성화.
 */
@SpringBootTest
@ActiveProfiles("e2e")
@TestPropertySource(
    properties = {
      "youthmoa.mail.mock=false",
      "spring.mail.host=127.0.0.1",
      "spring.mail.port=3025",
      "spring.mail.properties.mail.smtp.auth=false",
      "spring.mail.properties.mail.smtp.starttls.enable=false",
      "youthmoa.mail.from-address=noreply@youth-moa.test",
      "youthmoa.mail.from-name=청년몽땅",
      "youthmoa.mail.login-url=http://localhost:8090/login",
      "youthmoa.mail.service-name=청년몽땅"
    })
class AdminInvitationMailServiceTest {

  // 고정 포트 3025 — @TestPropertySource 의 spring.mail.port 와 일치시켜야 서비스가 이 서버로 접속한다.
  // dynamicPort() 는 임의 포트를 잡아 spring context 에서 알 방법이 없어 사용 불가.
  // 로컬 개발 환경에 3025 를 쓰는 서비스가 없다고 가정 (MailHog 기본은 1025, GreenMail 관례는 3025).
  @RegisterExtension
  static GreenMailExtension greenMail =
      new GreenMailExtension(new ServerSetup(3025, "127.0.0.1", "smtp"));

  @Autowired AdminInvitationMailService mailService;

  @Test
  void sendInvitation_delivers_mime_message_with_expected_content() throws Exception {
    // GreenMail dynamic port 를 서버 property 로 재주입 (일부 CI 환경 대비 로그).
    // Spring context 는 이미 @TestPropertySource 로 3025 지정되어 있어 dynamicPort 가 3025 를 잡음.
    MailDispatchResult result =
        mailService.sendInvitation("recipient@test.local", "홍길동", "Abcd1234!@#$");

    assertThat(result.sent()).isTrue();
    assertThat(result.failureReason()).isNull();

    greenMail.waitForIncomingEmail(5000, 1);
    MimeMessage[] received = greenMail.getReceivedMessages();
    assertThat(received).hasSize(1);

    MimeMessage m = received[0];
    // Subject
    assertThat(m.getSubject()).contains("청년몽땅").contains("관리자 계정");
    // From (표시명 포함)
    assertThat(m.getFrom()[0].toString()).contains("noreply@youth-moa.test");
    // To
    assertThat(m.getAllRecipients()[0].toString()).isEqualTo("recipient@test.local");

    // Body (HTML) — 이름·이메일·임시 비밀번호·CTA 링크 렌더 확인.
    // SMTP 전송 시 한글은 quoted-printable 로 인코딩되므로 raw body 대신 decode 후 검사한다.
    String body = decodeBody(m);
    assertThat(body).contains("홍길동");
    assertThat(body).contains("recipient@test.local");
    assertThat(body).contains("Abcd1234!@#$");
    assertThat(body).contains("http://localhost:8090/login");
    assertThat(body).contains("로그인 하러 가기");
  }

  @Test
  void sendPasswordReset_delivers_reset_template_content() throws Exception {
    MailDispatchResult result =
        mailService.sendPasswordReset("user@test.local", "김민준", "NewPass9876!");

    assertThat(result.sent()).isTrue();

    greenMail.waitForIncomingEmail(5000, 1);
    MimeMessage[] received = greenMail.getReceivedMessages();
    assertThat(received).hasSize(1);

    MimeMessage m = received[0];
    assertThat(m.getSubject()).contains("임시 비밀번호").contains("재발급");

    String body = decodeBody(m);
    assertThat(body).contains("김민준");
    assertThat(body).contains("NewPass9876!");
    // 재발급 템플릿 고유 문구
    assertThat(body).contains("임시 비밀번호를 재발급");
  }

  /**
   * MimeMessage → UTF-8 문자열. GreenMailUtil.getBody() 는 quoted-printable raw 를 그대로 준다.
   * Content-Transfer-Encoding 을 decode 해 원문 한글을 복원.
   */
  private static String decodeBody(MimeMessage m) throws Exception {
    // 헤더 명시적 확인. HTML text/html + quoted-printable 이므로 MimeUtility.decode 로 stream 처리.
    byte[] raw =
        com.icegreen.greenmail.util.GreenMailUtil.getBody(m).getBytes(StandardCharsets.US_ASCII);
    var in = new ByteArrayInputStream(raw);
    var decoded = MimeUtility.decode(in, "quoted-printable");
    var bytes = decoded.readAllBytes();
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
