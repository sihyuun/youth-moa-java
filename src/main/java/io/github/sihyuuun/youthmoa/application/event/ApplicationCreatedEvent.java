package io.github.sihyuuun.youthmoa.application.event;

/**
 * A7 admin 헤더 알림 벨 (2026-09-17): 신청 생성/재신청 도메인 이벤트.
 *
 * <p>사용자가 프로그램 신청에 성공하면 발행된다. 신규 row 생성뿐 아니라 CANCELLED → PENDING 재활용 경로에서도 함께 발행되어 관리자에게 "새 신청이
 * 들어왔다" 는 시그널이 동일하게 전달되도록 한다.
 *
 * <p>{@link ApplicationApprovedEvent} 와 동일 패턴 — record 로 primitive/String snapshot 만 담아
 * {@code @TransactionalEventListener(AFTER_COMMIT)} 리스너가 안전하게 사용하도록 한다. 엔티티 참조는
 * LazyInitializationException 위험이 있어 담지 않는다.
 *
 * <p>{@code programOrganization} 필드는 A7 QC (B-1) 수신자 결정에 사용된다: {@code
 * ApplicationCreatedRecipientResolver} 가 이 값과 {@code CENTER_ADMIN.effectiveCenterName()} 문자열을 매칭해
 * fan-out 대상을 결정.
 */
public record ApplicationCreatedEvent(
    Long applicationId,
    Long userId,
    Long programId,
    String programTitle,
    String programOrganization) {}
