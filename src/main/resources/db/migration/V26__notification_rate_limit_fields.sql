-- A7-rate-limit (2026-09-29): 5분 이내 동일 dedup_key 알림 병합용 컬럼.
--
-- 정책 (Q1~Q6 사용자 결정):
--   Q1 병합형 (count 보존)   Q2 기본 5분 (properties override)   Q3 그룹기준 = type+sourceId
--   Q4 UI 부기 = [n건] title   Q5 사용자 트랙 무영향, admin 트랙만   부수: 병합 시 isRead=false 로 리셋
--
-- 컬럼:
--   dedup_key         : 병합 대상 그룹 키. 형식 "NEW_APPLICATION:{programId}" / "NEW_USER:global".
--                       nullable — 사용자 트랙(NotificationService.create) 은 null 로 저장 → 병합 대상 아님.
--   occurrence_count  : 병합된 원시 이벤트 수. UI 는 > 1 일 때 "[n건] title" 부기.
--   last_occurred_at  : 마지막 발생 시각. 병합 판정 window (`now - 5min`) 및 헤더 드롭다운 최근순 정렬 기준.
--
-- 인덱스:
--   idx_notification_dedup : (user_id, type, dedup_key, last_occurred_at DESC) — findMergeCandidate 조회 커버.

ALTER TABLE notification ADD COLUMN dedup_key VARCHAR(120);
ALTER TABLE notification ADD COLUMN occurrence_count INTEGER NOT NULL DEFAULT 1;
ALTER TABLE notification ADD COLUMN last_occurred_at TIMESTAMP;

-- 기존 row backfill: created_at 을 last_occurred_at 으로 승격 (병합 판정 시 window 밖으로 자연 폴백).
UPDATE notification SET last_occurred_at = created_at WHERE last_occurred_at IS NULL;

ALTER TABLE notification ALTER COLUMN last_occurred_at SET NOT NULL;

CREATE INDEX idx_notification_dedup
    ON notification (user_id, type, dedup_key, last_occurred_at DESC);
