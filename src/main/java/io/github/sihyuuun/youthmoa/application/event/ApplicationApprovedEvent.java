package io.github.sihyuuun.youthmoa.application.event;

/**
 * 신청 승인 도메인 이벤트.
 *
 * <p>{@code record} 로 primitive/String snapshot 을 담아 트랜잭션 커밋 이후 {@code @TransactionalEventListener}
 * 에서 안전하게 사용할 수 있게 한다. 엔티티를 그대로 담으면 LazyInitializationException 위험이 있어 스칼라 스냅샷만 보관한다.
 */
public record ApplicationApprovedEvent(
    Long applicationId, Long userId, Long programId, String programTitle) {}
