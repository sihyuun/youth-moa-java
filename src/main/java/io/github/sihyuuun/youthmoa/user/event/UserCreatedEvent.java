package io.github.sihyuuun.youthmoa.user.event;

/**
 * A7 admin 헤더 알림 벨 (2026-09-17): 신규 회원가입 도메인 이벤트.
 *
 * <p>{@code UserService.signUp(...)} 성공 시 발행된다. 수신자: SYSTEM_ADMIN 전원 (Qn-E). CENTER_ADMIN 은 회원가입
 * 시점에 사용자 소속 center 정보가 없어 자기 센터 사용자인지 판단할 근거가 없으므로 발송하지 않는다.
 *
 * <p>record snapshot 규칙은 {@link
 * io.github.sihyuuun.youthmoa.application.event.ApplicationCreatedEvent} 와 동일.
 * LazyInitializationException 방지 목적.
 */
public record UserCreatedEvent(Long userId, String userName, String userEmail) {}
