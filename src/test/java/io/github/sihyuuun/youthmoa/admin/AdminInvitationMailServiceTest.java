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
 *
 * <p>2026-09-30 갱신 — 사용자 제공 mail 템플릿 편입에 따른 assertion 대응:
 *
 * <ul>
 *   <li>템플릿 내 서비스명은 "청년모아" 로 하드코딩됨 → subject 는 여전히 {@code serviceName} 사용 (여기서는 "청년몽땅").
 *   <li>초대 flow: 새 카피 "관리자 계정이 준비됐어요" · "청년모아 관리자로 초대" · CTA "청년모아로 이동하기".
 *   <li>재발급 flow: 새 카피 "비밀번호가 재설정됐어요" · "관리자가 비밀번호를 재설정".
 *   <li>centerName · resetBy 는 nullable — 각각 있음/없음 2 시나리오.
 *   <li>supportEmail 은 footer mailto 에 노출.
 * </ul>
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
      "youthmoa.mail.service-name=청년몽땅",
      "youthmoa.mail.support-email=help@youth-moa.test"
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
  void sendInvitation_with_centerName_delivers_full_template() throws Exception {
    MailDispatchResult result =
        mailService.sendInvitation("recipient@test.local", "홍길동", "Abcd1234!@#$", "고천센터");

    assertThat(result.sent()).isTrue();
    assertThat(result.failureReason()).isNull();

    greenMail.waitForIncomingEmail(5000, 1);
    MimeMessage[] received = greenMail.getReceivedMessages();
    assertThat(received).hasSize(1);

    MimeMessage m = received[0];
    // Subject 는 serviceName 기반 — 사용자 템플릿 편입 후에도 subject 조립은 코드 소관.
    assertThat(m.getSubject()).contains("청년몽땅").contains("관리자 계정이 준비됐어요");
    // From (표시명 포함)
    assertThat(m.getFrom()[0].toString()).contains("noreply@youth-moa.test");
    // To
    assertThat(m.getAllRecipients()[0].toString()).isEqualTo("recipient@test.local");

    // Body (HTML) — 새 템플릿 카피·loginId·tempPassword·centerName·CTA·supportEmail 렌더 확인.
    String body = decodeBody(m);
    assertThat(body).contains("홍길동");
    assertThat(body).contains("recipient@test.local"); // loginId row
    assertThat(body).contains("Abcd1234!@#$");
    assertThat(body).contains("고천센터"); // centerName row 노출
    assertThat(body).contains("관리자 계정이 준비됐어요"); // headline
    assertThat(body).contains("청년모아 관리자로 초대"); // body 카피
    assertThat(body).contains("http://localhost:8090/login");
    assertThat(body).contains("청년모아로 이동하기"); // CTA 텍스트
    assertThat(body).contains("help@youth-moa.test"); // supportEmail footer
  }

  @Test
  void sendInvitation_without_centerName_hides_center_row() throws Exception {
    // centerName=null → 템플릿 th:if="${centerName != null}" 로 소속 센터 row 미렌더.
    MailDispatchResult result =
        mailService.sendInvitation("solo@test.local", "김민준", "Pw@Solo0001", null);

    assertThat(result.sent()).isTrue();

    greenMail.waitForIncomingEmail(5000, 1);
    MimeMessage[] received = greenMail.getReceivedMessages();
    assertThat(received).hasSize(1);

    String body = decodeBody(received[0]);
    assertThat(body).contains("김민준");
    assertThat(body).contains("solo@test.local");
    assertThat(body).contains("Pw@Solo0001");
    // 소속 센터 라벨 자체는 label row 가 제거되므로 body 에 존재해선 안 됨.
    assertThat(body).doesNotContain("소속 센터");
  }

  @Test
  void sendPasswordReset_with_resetBy_delivers_full_template() throws Exception {
    MailDispatchResult result =
        mailService.sendPasswordReset("user@test.local", "김민준", "NewPass9876!", "김관리 (시스템 관리자)");

    assertThat(result.sent()).isTrue();

    greenMail.waitForIncomingEmail(5000, 1);
    MimeMessage[] received = greenMail.getReceivedMessages();
    assertThat(received).hasSize(1);

    MimeMessage m = received[0];
    assertThat(m.getSubject()).contains("청년몽땅").contains("비밀번호가 재설정됐어요");

    String body = decodeBody(m);
    assertThat(body).contains("김민준");
    assertThat(body).contains("user@test.local");
    assertThat(body).contains("NewPass9876!"); // newPassword 변수 렌더
    assertThat(body).contains("김관리 (시스템 관리자)"); // resetBy row
    // 재발급 템플릿 고유 문구
    assertThat(body).contains("비밀번호가 재설정됐어요"); // headline
    assertThat(body).contains("관리자가 비밀번호를 재설정"); // body 카피
    assertThat(body).contains("help@youth-moa.test");
  }

  @Test
  void sendPasswordReset_without_resetBy_hides_resetBy_row() throws Exception {
    MailDispatchResult result =
        mailService.sendPasswordReset("anon@test.local", "이서연", "Rst!Anon0001", null);

    assertThat(result.sent()).isTrue();

    greenMail.waitForIncomingEmail(5000, 1);
    MimeMessage[] received = greenMail.getReceivedMessages();
    assertThat(received).hasSize(1);

    String body = decodeBody(received[0]);
    assertThat(body).contains("이서연");
    assertThat(body).contains("Rst!Anon0001");
    // resetBy=null → "재설정한 관리자" label row 렌더 안 됨.
    assertThat(body).doesNotContain("재설정한 관리자");
  }

  /**
   * MimeMessage → UTF-8 문자열. GreenMailUtil.getBody() 는 quoted-printable raw 를 그대로 준다.
   * Content-Transfer-Encoding 을 decode 해 원문 한글을 복원.
   */
  private static String decodeBody(MimeMessage m) throws Exception {
    byte[] raw =
        com.icegreen.greenmail.util.GreenMailUtil.getBody(m).getBytes(StandardCharsets.US_ASCII);
    var in = new ByteArrayInputStream(raw);
    var decoded = MimeUtility.decode(in, "quoted-printable");
    var bytes = decoded.readAllBytes();
    return new String(bytes, StandardCharsets.UTF_8);
  }
}
