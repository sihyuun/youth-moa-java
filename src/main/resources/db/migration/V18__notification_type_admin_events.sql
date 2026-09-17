-- A7 admin 헤더 알림 벨 (2026-09-17)
-- NotificationType enum 확장: NEW_APPLICATION, NEW_USER
-- 엔티티는 @Enumerated(STRING) 이라 컬럼 스키마 변경 없음 — 문서화·이력 추적성 목적의 comment 갱신만 수행.
-- 사용자 알림 flow (PR #143) 무회귀: 기존 enum 값 6종 그대로 유지.
COMMENT ON COLUMN notification.type IS
  'NotificationType enum: APPLICATION_APPROVED/REJECTED/CANCELLED, WAITLIST_PROMOTED, PROGRAM_DEADLINE_NEAR, WELCOME, NEW_APPLICATION (A7), NEW_USER (A7)';
