package io.github.sihyuuun.youthmoa.admin;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * A5-1-e2e Part 1 Q6 (2026-10-16) — {@link MockAdminInvitationMailSender} 단위 테스트.
 *
 * <p>검증 대상 (명세 §6 Q6 A안 — 소규모 단위 테스트):
 *
 * <ul>
 *   <li>정상 발송 시 log 에 마스킹된 password 만 포함되고 raw password 는 포함되지 않는다 — maskPassword 는 보안 (로그 유출 방어)
 *       코드이므로 직접 검증.
 *   <li>{@link MockAdminInvitationMailSender#setForceFail(boolean)} ON 상태에서는 failure 결과를 돌려주며 fail
 *       로그 포맷이 사용된다.
 *   <li>setForceFail 토글 반복 호출 안전 (idempotent).
 *   <li>null · 짧은 password 입력 시 NPE 없이 안전하게 처리된다.
 * </ul>
 *
 * <p>구현 노트: {@code maskPassword} 는 private static 이라 reflection 대신 공개 API ({@code sendInvitation} ·
 * {@code sendPasswordReset}) 호출 → logback {@link ListAppender} 로 수집된 로그 메시지를 검증한다. 이렇게 하면 "mask 패턴"
 * 자체뿐 아니라 "실제 발송 경로가 올바른 포맷으로 로그를 남기는가" 까지 함께 지켜진다.
 */
class MockAdminInvitationMailSenderTest {

  private MockAdminInvitationMailSender sender;
  private Logger logger;
  private ListAppender<ILoggingEvent> appender;

  @BeforeEach
  void setUp() {
    sender = new MockAdminInvitationMailSender();
    logger = (Logger) LoggerFactory.getLogger(MockAdminInvitationMailSender.class);
    appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    logger.setLevel(Level.INFO);
  }

  @AfterEach
  void tearDown() {
    logger.detachAppender(appender);
    appender.stop();
    // 다른 테스트 오염 방지 — mock sender 는 매번 신규 생성이지만 명시적으로 off.
    sender.setForceFail(false);
  }

  @Test
  void send_normal_logs_masked_password() {
    String rawPassword = "SuperSecret9!";

    var result = sender.sendInvitation("new-admin@youth-moa.test", "홍길동", rawPassword, "중앙센터");

    assertThat(result.sent()).isTrue();
    String joinedLogs = joinFormattedMessages();
    // mask 패턴: 앞 2자 + "***"
    assertThat(joinedLogs).contains("Su***");
    // raw password 가 로그에 노출되지 않아야 함 (보안 핵심 가드)
    assertThat(joinedLogs).doesNotContain(rawPassword);
  }

  @Test
  void send_force_fail_enabled_returns_failure_result() {
    sender.setForceFail(true);

    var invitation = sender.sendInvitation("fail@youth-moa.test", "실패대상", "AnyPass12!", "테스트센터");
    var reset = sender.sendPasswordReset("fail@youth-moa.test", "실패대상", "ResetPw34!", "sysadmin");

    assertThat(invitation.sent()).isFalse();
    assertThat(invitation.failureReason()).isNotBlank();
    assertThat(reset.sent()).isFalse();
    assertThat(reset.failureReason()).isNotBlank();

    String joinedLogs = joinFormattedMessages();
    assertThat(joinedLogs).contains("MOCK-FORCE-FAIL");
    // force-fail 경로에서도 raw password 는 로그에 찍히지 않아야 함
    assertThat(joinedLogs).doesNotContain("AnyPass12!");
    assertThat(joinedLogs).doesNotContain("ResetPw34!");
  }

  @Test
  void set_force_fail_toggle_idempotent() {
    // 초기 상태 off
    assertThat(sender.isForceFail()).isFalse();

    sender.setForceFail(true);
    sender.setForceFail(true);
    assertThat(sender.isForceFail()).isTrue();

    sender.setForceFail(false);
    sender.setForceFail(false);
    assertThat(sender.isForceFail()).isFalse();

    // on → off → on 재토글
    sender.setForceFail(true);
    assertThat(sender.isForceFail()).isTrue();
    sender.setForceFail(false);
    assertThat(sender.isForceFail()).isFalse();
  }

  @Test
  void mask_password_null_or_short_input_safe() {
    // null password — NPE 없이 동작, 로그에는 fallback mask ("***") 노출
    var r1 = sender.sendInvitation("edge@youth-moa.test", "엣지", null, null);
    assertThat(r1.sent()).isTrue();

    // 1자 password — length < 2 분기 (앞 2자 substring 안전성)
    var r2 = sender.sendPasswordReset("edge@youth-moa.test", "엣지", "a", "sysadmin");
    assertThat(r2.sent()).isTrue();

    // 빈 문자열
    var r3 = sender.sendInvitation("edge2@youth-moa.test", "엣지2", "", "센터");
    assertThat(r3.sent()).isTrue();

    String joinedLogs = joinFormattedMessages();
    // null/짧은 입력은 "***" 로 치환되어야 함 (앞 2자 사용 금지)
    assertThat(joinedLogs).contains("***");
  }

  /** 수집된 logging event 들을 포맷된 메시지로 결합. 다수 호출의 결과를 일괄 검증할 때 사용. */
  private String joinFormattedMessages() {
    StringBuilder sb = new StringBuilder();
    for (ILoggingEvent event : appender.list) {
      sb.append(event.getFormattedMessage()).append('\n');
    }
    return sb.toString();
  }
}
