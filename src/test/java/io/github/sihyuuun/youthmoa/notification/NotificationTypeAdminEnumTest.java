package io.github.sihyuuun.youthmoa.notification;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A7 admin 헤더 알림 벨 (2026-09-17): NotificationType enum 확장분 매핑 검증.
 *
 * <p>{@code getToneColor()}, {@code getIconName()} 은 exhaustive switch 라 새 값 누락 시 compile 이 감지한다. 이
 * 테스트는 매핑 값이 spec §4-1 표와 일치하는지 런타임 확인 + 기존 값 회귀 방어를 겸한다.
 */
class NotificationTypeAdminEnumTest {

  @Test
  @DisplayName("NEW_APPLICATION → primary/bell (spec §4-1)")
  void newApplication_tone_and_icon() {
    assertThat(NotificationType.NEW_APPLICATION.getToneColor()).isEqualTo("primary");
    assertThat(NotificationType.NEW_APPLICATION.getIconName()).isEqualTo("bell");
  }

  @Test
  @DisplayName("NEW_USER → success/check (spec §4-1)")
  void newUser_tone_and_icon() {
    assertThat(NotificationType.NEW_USER.getToneColor()).isEqualTo("success");
    assertThat(NotificationType.NEW_USER.getIconName()).isEqualTo("check");
  }

  @Test
  @DisplayName("사용자 트랙 기존 6종 매핑 무회귀 (PR #143 정합)")
  void existing_types_unchanged() {
    assertThat(NotificationType.APPLICATION_APPROVED.getToneColor()).isEqualTo("success");
    assertThat(NotificationType.APPLICATION_APPROVED.getIconName()).isEqualTo("check");
    assertThat(NotificationType.APPLICATION_REJECTED.getToneColor()).isEqualTo("error");
    assertThat(NotificationType.APPLICATION_REJECTED.getIconName()).isEqualTo("close");
    assertThat(NotificationType.APPLICATION_CANCELLED.getToneColor()).isEqualTo("error");
    assertThat(NotificationType.APPLICATION_CANCELLED.getIconName()).isEqualTo("close");
    assertThat(NotificationType.WAITLIST_PROMOTED.getToneColor()).isEqualTo("success");
    assertThat(NotificationType.WAITLIST_PROMOTED.getIconName()).isEqualTo("bell");
    assertThat(NotificationType.PROGRAM_DEADLINE_NEAR.getToneColor()).isEqualTo("warning");
    assertThat(NotificationType.PROGRAM_DEADLINE_NEAR.getIconName()).isEqualTo("calendar");
    assertThat(NotificationType.WELCOME.getToneColor()).isEqualTo("primary");
    assertThat(NotificationType.WELCOME.getIconName()).isEqualTo("bell");
  }

  @Test
  @DisplayName("switch exhaustive 검증 — 모든 enum 값이 매핑을 갖는다 (values() 순회)")
  void all_types_have_mapping() {
    for (NotificationType t : NotificationType.values()) {
      assertThat(t.getToneColor()).isNotNull().isNotBlank();
      assertThat(t.getIconName()).isNotNull().isNotBlank();
    }
  }
}
